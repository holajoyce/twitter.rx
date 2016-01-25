package uk.co.flax.luwak.presearcher;

import java.util.List;

import uk.co.flax.luwak.termextractor.QueryTreeBuilder;
import uk.co.flax.luwak.termextractor.treebuilder.*;
import uk.co.flax.luwak.util.CollectionUtils;

/*
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
 * PresearcherComponent defining QueryTreeBuilders for various Filter queries
 */
public class FilterQueryPresearcherComponent extends PresearcherComponent {

    private static final List<? extends QueryTreeBuilder<?>> BUILDERS = CollectionUtils.makeUnmodifiableList(
            new BooleanQueryTreeBuilder.FilterBuilder(),
            new FilteredQueryTreeBuilder(),
            new TermsFilterQueryTreeBuilder(),
            new TermFilterQueryTreeBuilder(),
            new GenericFilterQueryTreeBuilder()
    );

    public FilterQueryPresearcherComponent() {
        super(BUILDERS);
    }

}
