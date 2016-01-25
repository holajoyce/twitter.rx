package uk.co.flax.luwak.termextractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import uk.co.flax.luwak.presearcher.PresearcherComponent;
import uk.co.flax.luwak.termextractor.querytree.QueryTree;
import uk.co.flax.luwak.termextractor.querytree.TreeAdvancer;
import uk.co.flax.luwak.termextractor.querytree.TreeWeightor;
import uk.co.flax.luwak.termextractor.treebuilder.*;
import uk.co.flax.luwak.util.CollectionUtils;

/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
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

/**
 * Class to analyze and extract terms from a lucene query, to be used by
 * a {@link uk.co.flax.luwak.Presearcher} in indexing.
 *
 * QueryAnalyzer uses a {@link uk.co.flax.luwak.termextractor.querytree.TreeWeightor}
 * to choose which branches of a conjunction query to collect terms from.
 */
public class QueryAnalyzer {

    private final List<QueryTreeBuilder<?>> queryTreeBuilders;

    public static final List<QueryTreeBuilder<? extends Query>> DEFAULT_BUILDERS = CollectionUtils.makeUnmodifiableList(
            new BooleanQueryTreeBuilder.QueryBuilder(),
            new PhraseQueryTreeBuilder(),
            new ConstantScoreQueryTreeBuilder(),
            new NumericRangeQueryTreeBuilder(),
            new TermRangeQueryTreeBuilder(),
            new RegexpAnyTermQueryTreeBuilder(),
            new SimpleTermQueryTreeBuilder(),
            new SpanTermQueryTreeBuilder(),
            new SpanNearQueryTreeBuilder(),
            new SpanOrQueryTreeBuilder(),
            new SpanMultiTermQueryWrapperTreeBuilder(),
            new SpanNotQueryTreeBuilder(),
            new GenericQueryTreeBuilder()
    );

    public final TreeWeightor weightor;

    /**
     * Create a QueryAnalyzer using provided QueryTreeBuilders, in addition to the default set
     *
     * @param weightor a TreeWeightor to use for conjunctions
     * @param queryTreeBuilders QueryTreeBuilders used to analyze queries
     */
    public QueryAnalyzer(TreeWeightor weightor, List<QueryTreeBuilder<?>> queryTreeBuilders) {
        this.queryTreeBuilders = new ArrayList<>();
        this.queryTreeBuilders.addAll(queryTreeBuilders);
        this.queryTreeBuilders.addAll(DEFAULT_BUILDERS);
        this.weightor = weightor;
    }

    /**
     * Create a QueryAnalyzer using provided QueryTreeBuilders, in addition to the default set
     *
     * @param weightor a TreeWeightor to use for conjunctions
     * @param queryTreeBuilders QueryTreeBuilders used to analyze queries
     */
    public QueryAnalyzer(TreeWeightor weightor, QueryTreeBuilder<?>... queryTreeBuilders) {
        this(weightor, Arrays.asList(queryTreeBuilders));
    }

    /**
     * Build a new QueryAnalyzer using a TreeWeightor and a list of PresearcherComponents
     *
     * A list of QueryTreeBuilders is extracted from each component, and combined to use
     * on the QueryAnalyzer
     *
     * @param weightor a TreeWeightor
     * @param components a list of PresearcherComponents
     * @return a QueryAnalyzer
     */
    public static QueryAnalyzer fromComponents(TreeWeightor weightor, PresearcherComponent... components) {
        List<QueryTreeBuilder<?>> builders = new ArrayList<>();
        for (PresearcherComponent component : components) {
            builders.addAll(component.getQueryTreeBuilders());
        }
        return new QueryAnalyzer(weightor, builders);
    }

    /**
     * Build a new QueryAnalyzer using a list of PresearcherComponents
     *
     * A list of QueryTreeBuilders is extracted from each component, and combined to use
     * on the QueryAnalyzer with a default TreeWeightor.
     *
     * @param components a list of PresearcherComponents
     * @return a QueryAnalyzer
     */
    public static QueryAnalyzer fromComponents(PresearcherComponent... components) {
        return fromComponents(TreeWeightor.DEFAULT_WEIGHTOR, components);
    }

    /**
     * Create a QueryAnalyzer using the default TreeWeightor, and the provided QueryTreeBuilders,
     * in addition to the default set
     *
     * @param queryTreeBuilders QueryTreeBuilders used to analyze queries
     */
    public QueryAnalyzer(QueryTreeBuilder<?>... queryTreeBuilders) {
        this(TreeWeightor.DEFAULT_WEIGHTOR, queryTreeBuilders);
    }

    /**
     * Create a {@link QueryTree} from a passed in Query or Filter
     * @param luceneQuery the query to analyze
     * @return a QueryTree describing the analyzed query
     */
    @SuppressWarnings("unchecked")
    public QueryTree buildTree(Object luceneQuery) {
        for (QueryTreeBuilder queryTreeBuilder : queryTreeBuilders) {
            if (queryTreeBuilder.cls.isAssignableFrom(luceneQuery.getClass())) {
                return queryTreeBuilder.buildTree(this, luceneQuery);
            }
        }
        throw new UnsupportedOperationException("Can't build query tree from query of type " + luceneQuery.getClass());
    }

    /**
     * Collect terms from a QueryTree
     * @param queryTree the analyzed QueryTree to collect terms from
     * @return a list of QueryTerms
     */
    public List<QueryTerm> collectTerms(QueryTree queryTree) {
        List<QueryTerm> terms = new ArrayList<>();
        queryTree.collectTerms(terms, weightor);
        return terms;
    }

    /**
     * Collect terms from a lucene Query
     * @param luceneQuery the query to analyze and collect terms from
     * @return a list of QueryTerms
     */
    public List<QueryTerm> collectTerms(Query luceneQuery) {
        return collectTerms(buildTree(luceneQuery));
    }

    /**
     * Collect terms from a lucene Filter
     * @param luceneFilter the filter to analyze and collect terms from
     * @return a list of QueryTerms
     */
    public List<QueryTerm> collectTerms(Filter luceneFilter) {
        return collectTerms(buildTree(luceneFilter));
    }

    public boolean advancePhase(QueryTree queryTree, TreeAdvancer advancer) {
        return queryTree.advancePhase(weightor, advancer);
    }

    public String getAnyToken() {
        return "__ANYTOKEN__";
    }

}
