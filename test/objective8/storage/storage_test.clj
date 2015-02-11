(ns objective8.storage.storage_test
  (:require [midje.sweet :refer :all]
            [korma.core :as korma]
            [objective8.storage.storage :as s]
            [objective8.storage.mappings :as m]
            ))

(fact "attempts to store an object by looking up the entity mapping"
      (let [some-map {:foo "bar" :entity :i-am-entity}]
        (s/pg-store! some-map) => :the-id
        (provided
          (m/get-mapping some-map) => :fake-entity
          (s/insert :fake-entity anything) => :the-id)))

(fact "attempts to find an object based on a query containing entity"
      (let [some-query {:entity :i-am-entity :foo "bar" :zap "pow"}]
        (s/pg-retrieve some-query) => {:query some-query
                                       :result :expected-object }
        (provided
          (m/get-mapping some-query) => :fake-entity
          (s/select :fake-entity {:foo "bar" :zap "pow"}) => :expected-object)))

(fact "converts hyphens to underscores"
      (let [some-query {:entity :ent :foo-bar "wibble"}]
        (s/pg-retrieve some-query) => {:query some-query :result :expected-object}
        (provided
          (m/get-mapping some-query) => :fake-entity
          (s/select :fake-entity {:foo_bar "wibble"}) => :expected-object)))

(fact "throws exception if no entity key is present"
      (s/pg-retrieve {}) => (throws Exception "Query map requires an :entity key"))