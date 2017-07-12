(ns de.otto.status-test
  (:require [clojure.test :refer :all]
            [de.otto.status :as s]))


(def f-ok #(s/status-detail :ok-subcomponent :ok "all ok"))
(def f-ok2 #(s/status-detail :ok-subcomponent2 :ok "everything fine"))
(def f-warn #(s/status-detail :warn-subcomponent :warning "a warning"))
(def f-error #(s/status-detail :error-subcomponent :error "an error"))

(def forgiving-msgs {:ok    "at least one substatus ok"
                     :error "no substatus ok"})
(def strict-msgs {:ok      "all substatus ok"
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
           (s/aggregate-status :id s/forgiving-strategy [f-ok] {:extra-key "extra-value"}))))

  (testing "it returns a timeout status for status functions which did not finish fast enough"
    (let [ok-st-result ["myname" {:status :ok :message ""}]
          ok-st-fn (constantly ok-st-result)
          timeout-result ["myname" {:status :ok :message "SpCial"}]
          timeout-st-fn #(do (Thread/sleep 10) timeout-result)
          aggregated-status (get-in (s/aggregate-status :id s/forgiving-strategy [ok-st-fn timeout-st-fn] {} 5)
                        [:id :statusDetails])]
      (is (some #{:ok} (map :status (vals aggregated-status))))
      (is (some #{:timeout} (map :status (vals aggregated-status)))))))

(deftest timeouts-or-results-test
  (let [ok-st-result ["myname" {:status :ok :message ""}]
        ok-st-fn (constantly ok-st-result)
        timeout-result ["myname" {:status :ok :message "SpCial"}]
        timeout-st-fn #(do (Thread/sleep 10) timeout-result)]
    (testing "should return the result if status function finished in time"
      (is (= ok-st-result
             (s/result-or-timeout 100 ["ok-st-fn" (future (ok-st-fn))]))))

    (testing "should return a timeout result if status function did not finish in time"
      (is (= :timeout
             (:status (second (s/result-or-timeout 5 ["ok-st-fn" (future (timeout-st-fn))]))))))))