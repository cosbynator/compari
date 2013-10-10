package com.thomasdimson.wikipedia.lda.java;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

public class MarkupCleaner {
    public static final Set<String> STOP_WORDS = ImmutableSet.copyOf(new String[] {
            "!", ",", "the", "of", "and", "in", "a", "to", "-RCB-", "-LCB-", "-RRB-", "-LRB-", ":", "\\*", "is", "''",
            "as", "s", "for", "by", "was", "on", "that", "with", "title", "vcite", "cite", "from", "are",
            "?", ".", ";", "-", "it", "an", "or", "url", "at", "&", "his", "her", "be", "year",
            "this", "date", "accessdate", "he", "she", "they", "were", "not", "also", "web", "%", "\\", "have",
            "has", "one", "/", "+", "all", "some", "who", "what", "where", "when", "out", "d.", "s.", "so", "...",
            "n", "i", "its", "asl", "two", "three", "new", "use", "div", "col", "ref", "thus", "over", "left",
            "right", "but", "much", "name", "can", "doi",
            "last1", "last2", "last3", "last4", "last5", "last6", "last7", "last8", "last9",
            "first1", "first2", "first3", "first4", "first5", "first6", "first7", "first8", "first9",
            "author1", "author2", "author3", "author4", "author5", "author6", "author7", "author8", "author9",
            "dmy", "dates",  "those", "any", "however", "may", "category", "archiveurl", "aur", "pmc"
    });

    public static Set<String> readWhitelist(String filename, int index) throws IOException {
        Set<String> ret = Sets.newHashSet();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("UTF-8")));
        String line;
        while((line = r.readLine()) != null) {
            String []split = line.split("\\s+");
            if(index < split.length) {
                ret.add(split[index]);
            }
        }
        return ret;
    }

    public static final Pattern CLEAN_MARKUP_PATTERN = Pattern.compile(
            "(\\{|\\\\|\\|\\}|=|'|#|\\[|\\]|`|--|<|>)+"
    );
    public static final Pattern BAD_WORD_MATCH = Pattern.compile("^(\\d+|http).*");

    private static final PTBTokenizer.PTBTokenizerFactory<Word> tokenizerFactory
            = PTBTokenizer.PTBTokenizerFactory.newWordTokenizerFactory("asciiQuotes=true,normalizeOtherBrackets=true,escapeForwardSlashAsterisk=false");

    private Set<String> blacklist;
    private Set<String> whitelist;

    public MarkupCleaner(Set<String> blacklist, Set<String> whitelist) {
        this.blacklist = blacklist;
        this.whitelist = whitelist;
    }

    public String cleanMarkup(String text) {
        StringBuilder ret = new StringBuilder();
        String cleaned = CLEAN_MARKUP_PATTERN.matcher(text.toLowerCase()).replaceAll(" ");

        Iterator<Word> it = tokenizerFactory.getIterator(new StringReader(cleaned));
        while(it.hasNext()) {
            final String w = it.next().word().toLowerCase();
            if(w.length() >= 3 && w.length() <= 80 && whitelist.contains(w) && !w.contains(".") && !blacklist.contains(w) && !BAD_WORD_MATCH.matcher(w).matches()) {
                ret.append(w);
                ret.append(" ");
            }
        }

        return ret.toString();
    }
}
