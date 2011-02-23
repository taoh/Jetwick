/**
 * Copyright (C) 2010 Peter Karich <jetwick_@_pannous_._info>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jetwick.solr;

import de.jetwick.es.ElasticTweetSearch;
import de.jetwick.util.Helper;
import org.apache.solr.client.solrj.SolrQuery;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class JetwickQuery extends SolrQuery {

    public static void removeFacets(SolrQuery query) {
        query.setFacet(false);
        String[] facets = query.getFacetFields();
        if (facets != null)
            for (String str : facets) {
                query.removeFacetField(str);
            }
    }

    public JetwickQuery(boolean init) {
        if (init)
            init("");
    }

    public JetwickQuery(String queryStr) {
        init(queryStr);
    }

    public JetwickQuery() {
        init("");
    }

    public JetwickQuery setSort(String sortParam) {
        return (JetwickQuery) setSort(this, sortParam);
    }

    public static SolrQuery setSort(SolrQuery resQuery, String sortParam) {
        if(sortParam == null || sortParam.isEmpty())
            resQuery.remove("sort");
        else
            resQuery.set("sort", sortParam);
        return resQuery;
    }   

    /**
     * @deprecated use ElasticTweetSearch.findByTwitterId
     */
    public static SolrQuery createIdQuery(long id) {
        return new SolrQuery().addFilterQuery("id:" + id).setRows(2);
    }

    public JetwickQuery init(String queryStr) {
        if (queryStr == null)
            queryStr = "";
        queryStr = queryStr.replaceAll("--", "-").trim();
        if (queryStr.isEmpty() || queryStr.equals("*:*") || queryStr.equals("*"))
            queryStr = "";
        setQuery(queryStr);
        return attachFacetibility();
    }

    public JetwickQuery attachFacetibility() {        
        return this;
    }

    public JetwickQuery addUserFilter(String userName) {
        if (userName != null && !userName.trim().isEmpty()) {
            userName = trimUserName(userName);
            if (userName.contains(" "))
                userName = "\"" + userName + "\"";

            addFilterQuery(ElasticTweetSearch.KEY_USER + userName);
        }
        return this;
    }

    public static String getUserFilter(SolrQuery q) {
        String fq[] = q.getFilterQueries();
        if (fq != null) {
            for (String str : fq) {
                if (str.startsWith(ElasticTweetSearch.KEY_USER))
                    return str;
            }
        }
        return null;
    }

    public static String extractNonNullQueryString(SolrQuery query) {
        String visibleString = query.getQuery();
        if (visibleString != null && visibleString.length() > 0)
            return visibleString;

        return "";
    }

    public static String extractUserName(SolrQuery query) {
        String tmp = ElasticTweetSearch.KEY_USER;
        if (query.getFilterQueries() != null)
            for (String f : query.getFilterQueries()) {
                if (f.startsWith(tmp)) {                    
                    tmp = f.substring(tmp.length()).trim();
                    if(tmp.contains(" OR "))
                        return "";
                    if (tmp.length() > 1 && tmp.startsWith("\"") && tmp.endsWith("\""))
                        tmp = tmp.substring(1, tmp.length() - 1);
                    return trimUserName(tmp);
                }
            }

        return "";
    }

    public static String trimUserName(String userName) {
        userName = userName.toLowerCase();
        if (userName.startsWith("@"))
            userName = userName.substring(1);
        return userName;
    }

    public static void applyFacetChange(SolrQuery query, String filterQuery, boolean selected) {
        if (!filterQuery.contains(":")) {
            removeFilterQueries(query, filterQuery);
        } else if (selected)
            query.addFilterQuery(filterQuery);
        else
            query.removeFilterQuery(filterQuery);
    }

    public static void removeFilterQueries(SolrQuery query, String filterKey) {
        if (query.getFilterQueries() != null)
            for (String str : query.getFilterQueries()) {
                if (str.startsWith(filterKey + ":"))
                    query.removeFilterQuery(str);
            }
    }

    public static boolean containsFilter(SolrQuery query, String filter) {
        if (query.getFilterQueries() != null)
            for (String str : query.getFilterQueries()) {
                if (str.contains(filter))
                    return true;
            }
        return false;
    }

    public static boolean containsFilterKey(SolrQuery query, String filterKey) {
        return getFirstFilterQuery(query, filterKey) != null;
    }

    public static String getFirstFilterQuery(SolrQuery query, String filterKeyWithoutTag) {
        if (query.getFilterQueries() != null)
            for (String str : query.getFilterQueries()) {
                if (str.startsWith(filterKeyWithoutTag + ":"))
                    return str;
            }
        return null;
    }

    public static boolean expandFilterQuery(SolrQuery q, String filterWithoutTag, boolean addTag) {
        String filterKey = getFilterKey(filterWithoutTag);
        if (filterKey == null)
            return false;
        
        String filterToExpand = getFirstFilterQuery(q, filterKey);
        if (filterToExpand != null) {
            q.removeFilterQuery(filterToExpand);
            filterToExpand += " OR ";
        } else {
            filterToExpand = "";
        }

        q.addFilterQuery(filterToExpand + filterWithoutTag);
        return true;
    }

    public static boolean replaceFilterQuery(SolrQuery q, String filterWithoutTag, boolean addTag) {
        String filterKey = getFilterKey(filterWithoutTag);
        if (filterKey == null)
            return false;
        
        removeFilterQueries(q, filterKey);
        q.addFilterQuery(filterWithoutTag);
        return true;
    }

    public static String getFilterKey(String filter) {
        int index = filter.indexOf(":");
        if (index < 0)
            return null;

        return filter.substring(0, index);
    }

    /**
     * this method removes the specified filter from the first fq.
     * Example: reduceFilterQuery(q contains fq=test:hi OR test:me, test:hi);
     * after that q contains fq=test:me
     */
    public static boolean reduceFilterQuery(SolrQuery q, String filterWithoutTag) {
        int index = filterWithoutTag.indexOf(":");
        if (index < 0)
            return false;

        String filterKey = filterWithoutTag.substring(0, index);
        String filterToReduce = getFirstFilterQuery(q, filterKey);

        if (filterToReduce == null)
            return false;

        index = filterToReduce.indexOf(filterWithoutTag);
        if (index < 0)
            return false;

        String res[] = filterToReduce.split(" OR ");
        q.removeFilterQuery(filterToReduce);
        filterToReduce = "";
        int alreadyAdded = 0;
        for (int i = 0; i < res.length; i++) {            
            if (filterWithoutTag.equals(res[i]))
                continue;

            if (alreadyAdded++ > 0)
                filterToReduce += " OR ";

            filterToReduce += res[i];
        }

        if (!filterToReduce.isEmpty()) {            
            q.addFilterQuery(filterToReduce);
        }

        return true;
    }

    public static SolrQuery parse(String qString) {
        SolrQuery q = new SolrQuery();
        qString = Helper.urlDecode(qString);
        for (String str : qString.split("&")) {
            int index = str.indexOf("=");
            if (str.trim().isEmpty() || index < 0)
                continue;

            q.add(str.substring(0, index), str.substring(index + 1));
        }

        return q;
    }
}
