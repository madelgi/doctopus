(ns doctopus.doctopus
  "Maybe this is an awesome idea? Maybe this is a sub-good idea. We should
talk about it either way. The idea is: start defining an Entity we can flesh out.
Pin properties to. Give more shape.

My idea is that a Doctopus represents a *group* of docs. Maybe a collection of
repos, or all the docs for a single company, or all the docs for a functional
team within a company. Each Doctopus will have a Head -- a centralized notion
of what it is and what it does -- and some number of Tentacles -- individual
sources of documentation orchestrated by the Head.

Subject to all kinds of change. For instance: right now I've only really got a
gut feeling that a Doctopus needs a Head; I'm not sure I could precisely
articulate just _why_ yet.

  I think: Doctopus has several Heads, which each have many Tentacles. I think."
  (:require [doctopus.doctopus.head :as h]
            [doctopus.doctopus.tentacle :as t]))


;; Here there be Peculiar Metaphors for Information Dissemination
(defprotocol DoctopusMethods
  (list-tentacles [this])
  (list-tentacles-by-head [this head]))

(defrecord Doctopus
    [heads]
  DoctopusMethods
  (list-tentacles [this]
    (into {} (for [head (:heads this)
                   :let [tentacles (h/list-tentacles head)]]
               [head tentacles])))
  (list-tentacles-by-head [this head]
    {head (h/list-tentacles (:heads this))}))