/*
 *  Copyright 2010 Peter Karich jetwick_@_pannous_._info
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetwick.solr;

import de.jetwick.util.Helper;
import java.util.Collections;
import java.util.Date;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, jetwick_@_pannous_._info
 */
public class SavedSearchTest {

    public SavedSearchTest() {
    }

    @Test
    public void testQuery() {
        assertEquals("java, user:\"peter\"", new SavedSearch(1, new SolrQuery("java").addFilterQuery("user:\"peter\"")).getName());
        assertEquals("user:\"peter\"", new SavedSearch(1, new SolrQuery().addFilterQuery("user:\"peter\"")).getName());
        assertEquals("java, user:\"peter rich\"", new SavedSearch(1, new SolrQuery("java").addFilterQuery("user:\"peter rich\"")).getName());
        assertEquals("java termin, user:\"peter test\"", new SavedSearch(1, new SolrQuery("java termin").addFilterQuery("user:\"peter test\"")).getName());
    }
    @Test
    public void testGetQueryWithoutDateFilter() {
        assertNull(new SavedSearch(1, new SolrQuery().addFilterQuery("dt:[1 TO 2]")).getCleanQuery().getFilterQueries());
        assertEquals(1, new SavedSearch(1, new SolrQuery().addFilterQuery("dt:[1 TO 2]").
                addFilterQuery("xy:ab")).getCleanQuery().getFilterQueries().length);
    }

    @Test
    public void testAddFacet() {
        assertEndsWith("*:*", new SavedSearch(1, new SolrQuery()).calcFacetQuery());
        assertEndsWith("*:*", new SavedSearch(1, new SolrQuery("")).calcFacetQuery());
        assertEndsWith("tw:(peter) OR dest_title_t:(peter)", new SavedSearch(1, new SolrQuery("peter ")).calcFacetQuery());
        assertEndsWith("tw:(peter AND pan) OR dest_title_t:(peter AND pan)", new SavedSearch(1, new SolrQuery("peter pan")).calcFacetQuery());

        assertEndsWith("(tw:(peter AND pan) OR dest_title_t:(peter AND pan)) AND test:x",
                new SavedSearch(1, new SolrQuery("peter pan").addFilterQuery("test:x")).calcFacetQuery());
        assertEndsWith("(tw:(peter AND pan) OR dest_title_t:(peter AND pan)) AND test:x AND test2:y",
                new SavedSearch(1, new SolrQuery("peter pan").addFilterQuery("test:x").addFilterQuery("test2:y")).calcFacetQuery());
    }

    @Test
    public void testLastQueryDate() {
        SolrQuery q = new SolrQuery("wicket");
        SavedSearch ss = new SavedSearch(1, q);
        assertEndsWith("tw:(wicket) OR dest_title_t:(wicket)", ss.calcFacetQuery());

        Date date = ss.getLastQueryDate();
        assertNull(date);
        ss.getQuery(Collections.EMPTY_LIST);
        date = ss.getLastQueryDate();
        assertNotNull(date);
        assertEndsWith("tw:(wicket) OR dest_title_t:(wicket) AND dt:["
                + Helper.toLocalDateTime(date) + " TO *]", ss.calcFacetQuery());
    }

    public void assertEndsWith(String start, String str2) {
        assertTrue("expected end:" + start + " but was:" + str2, str2.endsWith(start));
    }
}