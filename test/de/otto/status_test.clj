(ns de.otto.status-test
  (:require [clojure.test :refer :all]
            [de.otto.status :as s]))


(def f-ok #(s/status-detail :ok-subcomponent :ok "all ok"))
(def f-ok2 #(s/status-detail :ok-subcomponent2 :ok "everything fine"))
(def f-warn #(s/status-detail :warn-subcomponent :warning "a warning"))
(def f-error #(s/status-detail :error-subcomponent :error "an error"))

(def forgiving-msgs {:ok      "at least one substatus ok"
                     :error   "no substatus ok"})
(def strict-msgs    {:ok      "all substatus ok"
                     :warning "at least one substatus warn. no error"
                     :error   "at least one substatus error"})

(deftest create-a-forgiving-aggregate-status
  (testing "ok if all ok"
    (let [status-map (merge (f-ok) (f-ok2))]
      (is (= {:status        :ok
              :message       "at least one substatus ok"
              :statusDetails status-map}
             (s/aggregate-forgiving forgiving-msgs status-map)))))

  (testing "ok if any ok"
    (let [status-map (merge (f-ok) (f-error))]
      (is (= {:status        :ok
              :message       "at least one substatus ok"
              :statusDetails status-map}
             (s/aggregate-forgiving forgiving-msgs status-map)))))

  (testing "error if none ok"
    (let [status-map (merge (f-warn) (f-error))]
      (is (= {:status        :error
              :message       "no substatus ok"
              :statusDetails status-map}
             (s/aggregate-forgiving forgiving-msgs status-map))))))

(deftest create-a-strict-aggregate-status
  (testing "ok if all ok"
    (let [status-map (merge (f-ok) (f-ok2))]
      (is (= {:status        :ok
              :message       "all substatus ok"
              :statusDetails status-map}
             (s/aggregate-strictly strict-msgs status-map)))))

  (testing "warn if any warning"
    (let [status-map (merge (f-ok) (f-warn))]
      (is (= {:status        :warning
              :message       "at least one substatus warn. no error"
              :statusDetails status-map}
             (s/aggregate-strictly strict-msgs status-map)))))

  (testing "error if any error"
    (let [status-map (merge (f-ok) (f-error) (f-warn))]
      (is (= {:status        :error
              :message       "at least one substatus error"
              :statusDetails status-map}
             (s/aggregate-strictly strict-msgs status-map))))))

(deftest create-an-aggregate-status-with-a-strategy
  (testing "forgiving-strategy is forgiving and has predefined messages"
    (is (= {:id {:status        :ok
                 :message       "at least one ok"
                 :statusDetails (merge (f-ok) (f-warn))}}
           (s/aggregate-status :id s/forgiving-strategy [f-ok f-warn]))))

  (testing "strict-strategy is strict and has predefined messages"
    (is (= {:id {:status        :warning
                 :message       "some warnings"
                 :statusDetails (merge (f-ok) (f-warn))}}
           (s/aggregate-status :id s/strict-strategy [f-ok f-warn]))))

  (testing "it keeps extra info"
    (is (= {:id {:status        :ok
                 :message       "at least one ok"
                 :extra-key     "extra-value"
                 :statusDetails (f-ok)}}
           (s/aggregate-status :id s/forgiving-strategy [f-ok] {:extra-key "extra-value"})))))
