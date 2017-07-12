(ns de.otto.status)

(defonce scores {:ok      0
                 :warning 1
                 :error   2
                 :timeout 3})

(defn aggregate-forgiving [msgs status-map]
  (let [best (apply min-key scores (map :status (vals status-map)))
        score (if (= :ok best) :ok :error)]
    (assoc {:status score :message (score msgs)}
      :statusDetails status-map)))

(defn aggregate-strictly [msgs status-map]
  (let [worst (apply max-key scores (map :status (vals status-map)))]
    (assoc {:status worst :message (worst msgs)}
      :statusDetails status-map)))

(defn status-detail
  ([id status message]
   (status-detail id status message {}))
  ([id status message extras]
   {id (assoc extras :status status :message message)}))

(defn forgiving-strategy [status-map]
  (aggregate-forgiving {:ok "at least one ok" :error "none ok"} status-map))

(defn strict-strategy [status-map]
  (aggregate-strictly {:ok "all ok" :warning "some warnings" :error "none ok"} status-map))

(defn result-or-timeout [timeout [st-fn-name future]]
  (let [result (deref future timeout [st-fn-name {:status  :timeout
                                                  :message (format "%s timed out after %dms!" st-fn-name timeout)}])]
    (when (= :timeout (:status (second result)))
      (future-cancel future))
    result))

(defn fn-name->future [funs]
  (into {} (map (fn [st-fn] [(str st-fn) (future (st-fn))]) funs)))

(defn aggregate-status
  ([id strategy funs] (aggregate-status id strategy funs {}))
  ([id strategy funs extras] (aggregate-status id strategy funs extras 500))
  ([id strategy funs extras timeout-ms]
   {id
    (into extras
          (if (empty? funs)
            {:status :ok :message "no substatus"}
            (strategy (into {} (map (partial result-or-timeout timeout-ms)
                                    (fn-name->future funs))))))}))
