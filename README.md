# status

A simple library that can be used to hierarchically aggretate status of different sources and subsources. A valid status consists of at least this info:

```edn
{:id-of-source {:status :ok :message "a message"}}
```
with status beeing one of ```[:ok :warning :error :timeout]```. The inner map can contain arbitrary addidtional information.

## Usage

Add ```[de.otto/status "0.1.0"]``` to your project dependencies. See example usage below.

## Example

To try this example, start a repl with

```$ lein repl```

and do something like this
```clj
(use 'de.otto.status)

;; you'll need some functions that return status in the given format.

;; either make the map yourself
(def fun1 (fn [] {:component-id1 {:status :ok :message "all ok"}}))

;; or use the factory function
(def fun2 #(status-detail :component-id2 :warning "a warning"))
```

There is two aggretion strategies: strict and forgiving. The strict one will always aggregate to the worst status:

```clj
(aggregate-status :app-id strict-strategy [fun1 fun2])
;; results in
{:app-id
 {:status :warning
   :message "at least one substatus warn. no error"
   :statusDetails
     {:component-id1 {:status :ok :message "all ok"}
      :component-id1 {:status :warning :message "a warning"}}}}
```

The forgiving strategy on the other hand will be fine if at least one substatus is ok.
```clj
(aggregate-status :app-id forgiving-strategy [fun1 fun2])
;; results in
{:app-id
 {:status :ok
   :message "at least one ok"
   :statusDetails
     {:component-id1 {:status :ok :message "all ok"}
      :component-id1 {:status :warning :message "a warning"}}}}

```

Note, that the resulting statuses could be aggregated again. Additional info can be added like this:

```clj
(aggregate-status :app-id strict-strategy [fun1] {:key "value"})
;; results in
{:app-id
 {:status :ok
   :message "all ok."
   :key "value"
   :statusDetails
     {:component-id1 {:status :ok :message "all ok"}}}}
```

See [tesla-microservice](https://github.com/otto-de/tesla-microservice) as an example of how to use status in a component based application.


## Initial Contributors

Christian Stamm, Felix Bechstein, Ralf Sigmund, Kai Brandes, Florian Weyandt

## License

Apache License
