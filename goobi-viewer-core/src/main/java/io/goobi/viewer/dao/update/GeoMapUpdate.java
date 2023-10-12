/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.maps.FeatureSet;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.maps.ManualFeatureSet;
import io.goobi.viewer.model.maps.SolrFeatureSet;

public class GeoMapUpdate implements IModelUpdate {

    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        if (dao.columnsExists("cms_geomap", "solr_query")) {

            Map<Long, FeatureSet> featureSets = createFeatureSets(dao);

            List<GeoMap> maps = addToGeomap(featureSets, dao);

            dao.executeUpdate("DELETE FROM cms_geomap_features");

            for (GeoMap geoMap : maps) {
                dao.updateGeoMap(geoMap);
            }

            dao.executeUpdate("ALTER TABLE cms_geomap DROP COLUMN solr_query");

            return true;
        }
        return false;
    }

    /**
     * 
     * @param featureSets
     * @param dao
     * @return
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    List<GeoMap> addToGeomap(Map<Long, FeatureSet> featureSets, IDAO dao) throws DAOException {
        List<GeoMap> maps = new ArrayList<>();
        for (Entry<Long, FeatureSet> entry : featureSets.entrySet()) {
            Long geomapId = entry.getKey();
            FeatureSet featureSet = entry.getValue();

            GeoMap geomap = dao.getGeoMap(geomapId);
            geomap.addFeatureSet(featureSet);
            if (featureSet instanceof ManualFeatureSet) {
                List<String> features = dao.getNativeQueryResults("SELECT features FROM cms_geomap_features WHERE geomap_id = " + geomapId + ";");
                ((ManualFeatureSet) featureSet).setFeatures(features);
            }
            maps.add(geomap);
        }

        return maps;
    }

    /**
     * 
     * @param dao
     * @return
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    Map<Long, FeatureSet> createFeatureSets(IDAO dao) throws DAOException {
        List<Object[]> info = dao.getNativeQueryResults("SHOW COLUMNS FROM cms_geomap");

        List<Object[]> geomaps = dao.getNativeQueryResults("SELECT * FROM cms_geomap");

        List<String> columnNames = info.stream().map(o -> (String) o[0]).collect(Collectors.toList());

        Map<Long, FeatureSet> featureSets = new HashMap<>();
        for (Object[] geomap : geomaps) {

            Map<String, Object> columns = IntStream.range(0, columnNames.size())
                    .boxed()
                    .filter(i -> geomap[i] != null)
                    .collect(Collectors.toMap(i -> columnNames.get(i), i -> geomap[i]));

            Long geomapId = Optional.ofNullable(columns.get("geomap_id")).map(o -> (Long) o).orElse(null);
            Integer mapType = Optional.ofNullable(columns.get("map_type")).map(o -> (Integer) o).orElse(null);
            String solrQuery = Optional.ofNullable(columns.get("solr_query")).map(o -> (String) o).orElse(null);
            String markerTitleField = Optional.ofNullable(columns.get("marker_title_field")).map(o -> (String) o).orElse(null);
            String marker = Optional.ofNullable(columns.get("marker")).map(o -> (String) o).orElse(null);

            if (mapType == null) {
                //skip
            } else if (mapType == 1) {
                ManualFeatureSet featureSet = new ManualFeatureSet();
                featureSet.setMarker(marker);
                featureSets.put(geomapId, featureSet);
            } else {
                SolrFeatureSet featureSet = new SolrFeatureSet();
                featureSet.setMarker(marker);
                featureSet.setSolrQuery(solrQuery);
                featureSet.setMarkerTitleField(markerTitleField);
                featureSets.put(geomapId, featureSet);
            }

        }

        return featureSets;
    }

}
