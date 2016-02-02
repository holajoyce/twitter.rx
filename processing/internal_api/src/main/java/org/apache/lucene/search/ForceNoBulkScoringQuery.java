package org.apache.lucene.search;
/*
 *   Copyright (c) 2015 Lemur Consulting Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;

/**
 * Query wrapper that forces its wrapped Query to use the default doc-by-doc
 * BulkScorer.
 */
public class ForceNoBulkScoringQuery extends Query {

    private final Query inner;

    public ForceNoBulkScoringQuery(Query inner) {
        this.inner = inner;
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query rewritten = inner.rewrite(reader);
        if (rewritten != inner)
            return new ForceNoBulkScoringQuery(rewritten);
        return super.rewrite(reader);
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {

        final Weight innerWeight = inner.createWeight(searcher, needsScores);

        return new Weight(ForceNoBulkScoringQuery.this) {
            @Override
            public void extractTerms(Set<Term> set) {
                innerWeight.extractTerms(set);
            }

            @Override
            public Explanation explain(LeafReaderContext leafReaderContext, int i) throws IOException {
                return innerWeight.explain(leafReaderContext, i);
            }

            @Override
            public float getValueForNormalization() throws IOException {
                return innerWeight.getValueForNormalization();
            }

            @Override
            public void normalize(float v, float v1) {
                innerWeight.normalize(v, v1);
            }

            @Override
            public Scorer scorer(LeafReaderContext leafReaderContext) throws IOException {
                return innerWeight.scorer(leafReaderContext);
            }
        };
    }

    @Override
    public String toString(String s) {
        return "NoBulkScorer(" + inner.toString(s) + ")";
    }
}
