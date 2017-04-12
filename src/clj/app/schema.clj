(ns app.schema
  (:require [datomic.api :as d]))

(def users
  [{:db/ident :user/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/encrypted-password
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(def word
  [{:db/ident :word/stem
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/index true}
   {:db/ident :word/word
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value}
   {:db/ident :word/dictionary
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :word/shorteningPos
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :word/articles
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many
    }
   ])

(def article
   [{:db/ident :article/article
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :article/accentPos
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
    ])
