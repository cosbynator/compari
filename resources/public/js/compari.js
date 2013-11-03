var delay = (function(){
  var timer = 0;
  return function(callback, ms){
    clearTimeout (timer);
    timer = setTimeout(callback, ms);
  };
})();

$(document).ready(function() {
    "use strict";

    var spinner = new Spinner();
    
    function parseExplanation(parse) {
        var start;
        switch(parse.type) {
            case "knn":
                return "K-Nearest Neighbors of <em>" + parse["article-title"] + "</em>";
            case "compare":
                start = "Comparing <em>" + parse["first-article-title"] + "</em>" + 
                                " with <em> "+ parse["second-article-title"] + "</em>";
                if(parse.topics.length > 0) {
                    start += " in the topic matching <em>" + parse.topics.join(" or ") + "</em>";
                }
                return start;
            case "top_k":
                start = "Top K articles";
                if(parse.infobox !== null) {
                    start += " with infobox <em> " + parse.infobox + " </em>";
                }
                if(parse.topics.length > 0) {
                    start += " in the topic matching <em>" + parse.topics.join(" or ") + "</em>";
                }
                return start;
            default:
                return "No explanation";
        }
    }

    var helpers = {
        wikilink: function(title) {
            return "<a href=\"http://en.wikipedia.org/wiki/" + title.replace(" ", "_") + "\">" + title + "</a>";
        }
    };

    var comparisonTemplate = _.template(
        "<div class='compare-result text-center'>" +
            "<p>We estimate that <strong> <%= wikilink(t1) %> </strong> is</p>" +
            "<div class='compare-ratio'><%=ratio%></div>" +
            "<p>more influential than <strong> <%= wikilink(t2) %> </strong></p>" + 
        "</div>" +
        "<p class='small'><ul>" +
            "<li>LDA-Cosine: <%= (lda_cosine * 100).toFixed(1) %>%" +
            "<li>TSPR-Cosine: <%= (tspr_cosine * 100).toFixed(1) %>%" +
        "</ul></p>"
    );

    function renderComparison(o) {
        var ratio = o.ratio.toFixed(1) + "x";
        var t1 = o["first-article"].title;
        var t2 = o["second-article"].title;
        return comparisonTemplate($.extend(helpers, {
            t1: t1,
            t2: t2,
            ratio: ratio,
            lda_cosine: o["lda-cosine-similarity"],
            tspr_cosine: o["tspr-cosine-similarity"],
        }));
    }

    var topKTemplate = _.template(
        "<h2> The Top <%= infobox %> </h2>" +
        "<% topicFragment %>" +
        "<table class='table table-striped'>" +
            "<thead>" +
                "<tr><th> # </th> <th> <%=infobox%> </th> <th> Relative </th> </tr>" +
            "</thead>" +
            "<tbody>" +
            "<% _.each(articles, function(article) { %>" +
                "<tr>" +
                    "<td> <%= article.position %> </td> <td> <%= wikilink(article.title)%> </td>" +
                    "<td> <%= (article.relative * 100).toFixed(0) %>% </td>" +
                "</tr>" +
            "<% }); %>" +
            "</tbody>" +
        "</table>"
    );

    function renderTopK(o) {
        if(o.articles.length === 0) {
            return "No results found";
        }

        var topicFragment = "";
        if(o["topic-words"].length > 0) {
            topicFragment = "<p><strong>Topic</strong>: " + o["topic-words"].join(",") + "</p>";
        }

        var topic = o["topic-index"];
        var firstArticle = o.articles[0];
        var article;
        for(var i = 0; i < o.articles.length; i++) {
            article = o.articles[i];
            article.position = i+1;
            article.relative = article.tspr[topic] / firstArticle.tspr[topic];
        }

        return topKTemplate($.extend(helpers, {
            topicFragment: topicFragment,
            articles: o.articles,
            infobox: (o.infobox ? o.infobox : "Thing")
        }));
    }

    $("#query").keyup(function() {
        var self = this;
        delay(function() {
            $.get("/parse", {query: $(self).val()}, function(data) { 
                if(data.parse.type === "failure") {
                    $("#query-submit").prop('disabled', true);
                    var col = data.parse.explanation.column;
                    var start = $(self).val().substring(0, col);
                    var end = $(self).val().substring(col);
                    $("#parse").html("Error at col " + col +": &quot;<em>" + start + "</em><strong>&loz;&loz;</strong>" + end + "&quot;");
                } else {
                    $("#query-submit").prop('disabled', false);
                    $("#parse").html("Interpretation: " + parseExplanation(data.parse));
                }
            });
        }, 50); 
        return true;
    });

    $("#query-form").submit(function() {
        $("#result").html("");
        spinner.spin(document.getElementById("result"));
        $.get("/query", {query: $("#query").val()}, function(data) {
            console.log(data);
            var renderFn; 
            if(data && data.type) {
                switch(data.type) {
                    case "compare": 
                        renderFn = renderComparison;
                        break;
                    case "top_k":
                        renderFn = renderTopK;
                        break;
                }

                spinner.stop();
                if(renderFn) {
                    $("#result").html(renderFn(data));
                } else {
                    $("#result").html("Error :(");
                }
            }
        });
        return false;
    });
});
