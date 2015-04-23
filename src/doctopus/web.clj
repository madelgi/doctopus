(ns doctopus.web
  (:require [bidi.ring :as bidi :refer [->Resources]]
            [clojure.java.io :as io]
            [doctopus.configuration :refer [server-config]]
            [doctopus.template :as templates]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :as reload]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.stacktrace :as trace]
            [ring.util.response :as ring-response]
            [taoensso.timbre :as log]
            [clojure.walk :refer [keywordize-keys]]))

(defn wrap-error-page [handler]
  "Utility ring handler for when Stuff goes Sideways; returns a 500 and an error
  page"
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn four-oh-four
  [route]
  (log/debug "404ing on route:" route)
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (format "<h3>Could not find a page matching %s </h3>" route)})

(defn wrap-route-not-found
  [handler]
  (fn [request]
    (if-let [response (handler request)]
      response
      (four-oh-four (:uri request)))))

(defn serve-html
  "Just returns html, with the content-type set correctly"
  [template]
  (-> template
      (ring-response/response)
      (ring-response/content-type "text/html")))

(defn serve-index
  [_]
  (serve-html (templates/index)))

(defn serve-add-head-form
  [_]
  (serve-html (templates/add-head)))

(defn serve-add-tentacle-form
  [_]
  (serve-html (templates/add-tentacle)))

(defn add-head
  [request]
   (let [head-name (get (:form-params request) "name")]
     (serve-html (str "ADD A HEAD: " head-name))))

(defn add-tentacle
  [request]
   (let [params (keywordize-keys (:form-params request))]
     (serve-html
      (str "ADD A TENTACLE: " (:name params) " BELONGING TO: " (:head params)))))

;; Bidi routes are defined as nested sets of ""
(def routes ["/" {""             {:get serve-index}
                  "assets"       (->Resources {:prefix "public/assets"})
                  "index.html"   {:get serve-index}
                  "add-head"     {:get serve-add-head-form :post add-head}
                  "add-tentacle" {:get serve-add-tentacle-form :post add-tentacle}}])

(def application-handlers
  (bidi/make-handler routes))

(def application
  (-> (wrap-defaults application-handlers site-defaults)
      (wrap-route-not-found)
      (reload/wrap-reload)
      ((if (= (:env (server-config)) :production)
         wrap-error-page
         trace/wrap-stacktrace))))

;; # Http Server
;; This is what lifts the whole beast off the ground. Reads its configs out of
;; resources/configuration.edn
(defn -main
  []
  (let [{:keys [port]} (server-config)]
    (log/info "Starting HTTP server on port" port)
    (server/run-server application {:port port})))