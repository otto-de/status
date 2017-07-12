(defproject de.otto/status "0.1.3"
  :description "A simple library to aggregate status."
  :url "https://github.com/otto-de/status"
  :license { :name "Apache License 2.0"
             :url "http://www.apache.org/license/LICENSE-2.0.html"}
  :scm { :name "git"
 	       :url "https://github.com/otto-de/status"}
  :profiles {:dev {:plugins [[lein-release/lein-release "1.0.9"]]}}
  :lein-release {:deploy-via :clojars}
  :dependencies [[org.clojure/clojure "1.8.0"]])
