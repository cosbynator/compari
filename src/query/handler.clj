(ns query.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.core :as h]
            [query.core :as q]
  )
  (:use [hiccup.page :only [html5 include-css]])
)


(defn home [& {:keys [explanation]}]
  (html5
    [:head
      [:title "Campari"]
      (include-css 
        "/css/bootstrap.min.css"
        "/css/bootstrap-theme.min.css"
        "/css/jumbotron.css"
        "/css/campari.css"
      )
    ]

   [:body
    [:div {:class "container"}
      [:div {:class "jumbotron"}
        [:h1 "Campari"]
        [:form {:method "POST"}
          [:input {:type "text" :name "query" :class "query"}]
          [:input {:type "submit" :class "query-submit" :value "Query"}]
          [:p {:class "examples"} 
           "e.g. Who is similar to Albert Einstein?"
          ]
        ]
      ]
      [:div {:class "row marketing"} 
       (when explanation
         (str "Intepretation: " explanation)
       )
      ]
      [:div {:class "footer"}
        [:p "&copy; Thomas Dimson 2013"]
      ]
    ]
   ]
))

(defn- query-request [query]
  (let [query-template (q/extract-query-template query)]
     (if (not query-template)
         (home :explanation "Unable to parse query")
         (let [template-explanation (q/template-explanation query-template)]
          (home :explanation template-explanation)
         )
     )
  )
)


(defroutes app-routes
  (GET "/" [] (home))
  (POST "/" [query] (query-request query))
  (route/resources "/")
  (route/not-found "404 Not found")
)


(def app
  (handler/site app-routes))
