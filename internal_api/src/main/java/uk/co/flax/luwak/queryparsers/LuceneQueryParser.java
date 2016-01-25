package uk.co.flax.luwak.queryparsers;

/*
 * Copyright (c) 2013 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import uk.co.flax.luwak.MonitorQueryParser;

/**
 * A query parser that uses the default Lucene parser
 */
public class LuceneQueryParser implements MonitorQueryParser {

    private final String defaultField;
    private final Analyzer analyzer;

    /**
     * Creates a parser with a given default field and analyzer
     * @param defaultField the default field
     * @param analyzer an analyzer to use to analyzer query terms
     */
    public LuceneQueryParser(String defaultField, Analyzer analyzer) {
        this.defaultField = defaultField;
        this.analyzer = analyzer;
    }

    /**
     * Creates a parser using a lucene {@link StandardAnalyzer}
     * @param defaultField the default field
     */
    public LuceneQueryParser(String defaultField) {
        this(defaultField, new StandardAnalyzer());
    }

    @Override
    public Query parse(String query, Map<String, String> metadata) throws Exception {
        return new QueryParser(defaultField, analyzer).parse(query);
    }
}
