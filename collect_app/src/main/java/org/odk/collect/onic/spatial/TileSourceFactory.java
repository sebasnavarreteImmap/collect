package org.odk.collect.onic.spatial;

import android.content.Context;

import org.odk.collect.onic.R;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

/**
  * This factory augments Tile sources provides additional tile sources, some of which
  * are available in new version of osmdroid but not in currently used 4.2
 */
public class TileSourceFactory {
    public final OnlineTileSourceBase usgsTopo;
    public final OnlineTileSourceBase usgsSat;
    public final OnlineTileSourceBase stamenTerrain;
    public final OnlineTileSourceBase cartoDbPositron;
    public final OnlineTileSourceBase cartoDbDarkMatter;

    public TileSourceFactory(Context context) {
        usgsTopo = new OnlineTileSourceBase(
                context.getString(R.string.openmap_usgs_topo),
                0, 18, 256, "",
                new String[] { "http://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/tile/" }) {
            @Override
            public String getTileURLString(MapTile tile) {
                return getBaseUrl() + tile.getZoomLevel() + "/" + tile.getY() + "/" + tile.getX();
            }
        };

        usgsSat = new OnlineTileSourceBase(
                context.getString(R.string.openmap_usgs_sat),
                0, 18, 256, "",
                new String[]{"http://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer/tile/"}) {
            @Override
            public String getTileURLString(MapTile tile) {
                return getBaseUrl() + tile.getZoomLevel() + "/" + tile.getY() + "/" + tile.getX();
            }
        };

        stamenTerrain = new XYTileSource(context.getString(R.string.openmap_stamen_terrain),
                0, 18, 256, ".jpg", new String[] {
                "http://tile.stamen.com/terrain/" });

        cartoDbPositron = new XYTileSource(context.getString(R.string.openmap_cartodb_positron),
                0, 18, 256, ".png", new String[] {
                "http://1.basemaps.cartocdn.com/light_all/" });

        cartoDbDarkMatter = new XYTileSource(context.getString(R.string.openmap_cartodb_darkmatter),
                0, 18, 256, ".png", new String[] {
                "http://1.basemaps.cartocdn.com/dark_all/" });
    }

    public OnlineTileSourceBase getUSGSTopo() {
        return usgsTopo;
    }

    public OnlineTileSourceBase getUsgsSat() {
        return usgsSat;
    }

    public OnlineTileSourceBase getStamenTerrain() {
        return stamenTerrain;
    }

    public OnlineTileSourceBase getCartoDbPositron() {
        return cartoDbPositron;
    }

    public OnlineTileSourceBase getCartoDbDarkMatter() {
        return cartoDbDarkMatter;
    }
}
