package com.zhuke.util;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 国家-省-市-区相关信息
 * Created by ZHUKE on 2016/9/22.
 */
public class AreaUtils {

    private static final Logger logger = LoggerFactory.getLogger(AreaUtils.class);
    private static AreaInfo rootArea;

    /**
     * 获取国家列表
     */
    public static List<AreaInfoVo> getCountry() {
        if (rootArea == null) loadArea();
        return getSubListVo(rootArea, AreaLevel.COUNTRY);
    }

    /**
     * 根据指定的国家id获取省列表
     * <p>当获取STATE级别地区的zoomname为空时，向下级继续查找</p>
     *
     * @param countryId 国家id
     */
    public static List<AreaInfoVo> getState(String countryId) {
        AreaInfo country = findAreaInfoById(countryId, AreaLevel.COUNTRY);
        if (isTransitNull(country)) {
            country = findAreaInfoById(countryId, AreaLevel.STATE);
            if (isTransitNull(country)) {
                country = findAreaInfoById(countryId, AreaLevel.CITY);
                return country == null ? null : getSubListVo(country, AreaLevel.REGION);
            }
            return country == null ? null : getSubListVo(country, AreaLevel.CITY);
        }
        return country == null ? null : getSubListVo(country, AreaLevel.STATE);

    }


    /**
     * 根据指定的省id获取市列表
     *
     * @param stateId 省id
     */
    public static List<AreaInfoVo> getCity(String stateId) {
        AreaInfo state = findAreaInfoById(stateId, AreaLevel.STATE);
        if (isTransitNull(state)) {
            state = findAreaInfoById(stateId, AreaLevel.CITY);
            return state == null ? null : getSubListVo(state, AreaLevel.REGION);
        }
        return state == null ? null : getSubListVo(state, AreaLevel.CITY);
    }

    /**
     * 根据指定的市id获取区列表
     *
     * @param cityId 市id
     */
    public static List<AreaInfoVo> getRegion(String cityId) {
        AreaInfo city = findAreaInfoById(cityId, AreaLevel.CITY);
        return city == null ? null : getSubListVo(city, AreaLevel.REGION);
    }

    /**
     * 获取地区信息的下一级地区信息
     * <p>不支持跨级</>
     *
     * @param subLevel 下一级地区信息的级别
     * @return
     */
    private static List<AreaInfoVo> getSubListVo(AreaInfo areaInfo, AreaLevel subLevel) {
        List<AreaInfoVo> areaInfoVoList = new ArrayList<>();
        for (AreaInfo area : areaInfo.subArea) {
            AreaInfoVo vo = new AreaInfoVo(area.id, subLevel, area.zoomname);
            areaInfoVoList.add(vo);
        }
        return areaInfoVoList;
    }

    /**
     * 根据给定的地区id和级别查找地区信息
     */
    public static AreaInfo findAreaInfoById(String id, AreaLevel level) {
        if (rootArea == null) loadArea();
        for (AreaInfo country : rootArea.subArea) {
            if (country.id.equals(id) && country.level == level) {
                return country;
            } else if (id.startsWith(country.id) && country.level.ordinal() <= level.ordinal()) {
                for (AreaInfo state : country.subArea) {
                    if (state.id.equals(id) && state.level == level) return state;
                    else if (id.startsWith(country.id) && country.level.ordinal() <= level.ordinal()) {
                        for (AreaInfo city : state.subArea) {
                            if (city.id.equals(id) && city.level == level) return city;
                            else if (id.startsWith(city.id) && city.level.ordinal() <= level.ordinal()) {
                                for (AreaInfo region : city.subArea) {
                                    if (region.id.equals(id) && region.level == level) return region;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 根据areaId查找最后一级地区信息
     */
    public static String parseArea(String areaId) {
        AreaUtils.AreaInfo country = findAreaInfoById(areaId, AreaUtils.AreaLevel.COUNTRY);
        if (country != null && CollectionUtils.isEmpty(country.getSubArea())) return getAreaInfoStr(country);
        else {
            AreaUtils.AreaInfo state = findAreaInfoById(areaId, AreaUtils.AreaLevel.STATE);
            if (state != null && CollectionUtils.isEmpty(state.getSubArea())) return getAreaInfoStr(state);
            else {
                AreaInfo city = findAreaInfoById(areaId, AreaLevel.CITY);
                if (city != null && CollectionUtils.isEmpty(city.subArea)) return getAreaInfoStr(city);
                else {
                    AreaInfo region = findAreaInfoById(areaId, AreaLevel.REGION);
                    if (region != null) return getAreaInfoStr(region);
                }
            }
        }
        return null;
    }

    /**
     * 判断是否是中间过渡层，zoomname为null
     */
    private static boolean isTransitNull(AreaInfo areaInfo) {
        if (areaInfo.subArea.size() == 1) {
            return areaInfo.getSubArea().iterator().next().getZoomname() == null;
        }
        return false;
    }

    private static String getAreaInfoStr(AreaUtils.AreaInfo areaInfo) {
        return (areaInfo.getCountry() + ";" + areaInfo.getState() + ";" + areaInfo.getCity() + ";" + areaInfo.getRegion()).replace("null", "");
    }

    /**
     * 从文件中加载地区信息至内存
     */
    public static void loadArea() {
        try {
            Set<AreaInfo> countryAreaSet = new TreeSet<>();
            rootArea = new AreaInfo(null, AreaLevel.ROOT, null, null, null, null, null, null, countryAreaSet);

            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(ResourceUtils.getFile("classpath:LocList.xml"));

            Element root = document.getRootElement();
            List<Element> countryRegions = root.getChildren("CountryRegion");
            for (Element countryRegion : countryRegions) {
                String code = countryRegion.getAttributeValue("Code");
                String countryId = (code == null) ? "" : code;
                Set<AreaInfo> stateAreaSet = new TreeSet<>();
                String countryName = countryRegion.getAttributeValue("Name");
                AreaInfo countryArea = new AreaInfo(countryId, AreaLevel.COUNTRY, countryName, null, null, null, countryName, rootArea, stateAreaSet);
                countryAreaSet.add(countryArea);

                List<Element> states = countryRegion.getChildren("State");
                for (Element state : states) {
                    code = state.getAttributeValue("Code");
                    String stateId = countryId + (code == null ? "" : code);
                    Set<AreaInfo> cityAreaSet = new TreeSet<>();
                    String stateName = state.getAttributeValue("Name");
                    AreaInfo stateArea = new AreaInfo(stateId, AreaLevel.STATE, countryName, stateName, null, null, stateName, countryArea, cityAreaSet);
                    stateAreaSet.add(stateArea);

                    List<Element> cities = state.getChildren("City");
                    for (Element city : cities) {
                        code = city.getAttributeValue("Code");
                        String cityId = stateId + (code == null ? "" : code);
                        Set<AreaInfo> regionAreaSet = new TreeSet<>();
                        String cityName = city.getAttributeValue("Name");
                        AreaInfo cityArea = new AreaInfo(cityId, AreaLevel.CITY, countryName, stateName, cityName, null, cityName, stateArea, regionAreaSet);
                        cityAreaSet.add(cityArea);

                        List<Element> regions = city.getChildren("Region");
                        for (Element region : regions) {
                            code = region.getAttributeValue("Code");
                            String regionId = cityId + (code == null ? "" : code);
                            String regionName = region.getAttributeValue("Name");
                            AreaInfo regionArea = new AreaInfo(regionId, AreaLevel.REGION, countryName, stateName, cityName, regionName, regionName, cityArea, null);
                            regionAreaSet.add(regionArea);
                        }
                    }
                }
            }
        } catch (JDOMException | IOException e) {
            rootArea = null;
            logger.error("failed to load location file", e);
        }
    }

    /**
     * 地区信息
     */
    public static class AreaInfo implements Comparable {
        /**
         * 编号
         */
        private String id;
        /**
         * 级别
         */
        private AreaLevel level;
        /**
         * 国家
         */
        private String country;
        /**
         * 省
         */
        private String state;
        /**
         * 市
         */
        private String city;
        /**
         * 区
         */
        private String region;

        private String zoomname;
        /**
         * 父区域
         */
        private AreaInfo parentArea;
        /**
         * 子区域
         */
        private Set<AreaInfo> subArea;

        public AreaInfo() {
        }

        public AreaInfo(String id, AreaLevel level, String countryRegion, String state, String city, String region, String zoomname, AreaInfo parentArea, Set<AreaInfo> subArea) {
            this.id = id;
            this.level = level;
            this.country = countryRegion;
            this.state = state;
            this.city = city;
            this.region = region;
            this.parentArea = parentArea;
            this.subArea = subArea;
            this.zoomname = zoomname;
        }

        @Override
        public int compareTo(Object o) {
            if (this.id.length() != ((AreaInfo) o).id.length()) {
                return this.id.length() - ((AreaInfo) o).id.length();
            } else {
                return (this.id == null ? "" : this.id).compareTo((((AreaInfo) o).id == null ? "" : ((AreaInfo) o).id));
            }
        }

        public String getZoomname() {
            return zoomname;
        }

        public void setZoomname(String zoomname) {
            this.zoomname = zoomname;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public AreaLevel getLevel() {
            return level;
        }

        public void setLevel(AreaLevel level) {
            this.level = level;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public AreaInfo getParentArea() {
            return parentArea;
        }

        public void setParentArea(AreaInfo parentArea) {
            this.parentArea = parentArea;
        }

        public Set<AreaInfo> getSubArea() {
            return subArea;
        }

        public void setSubArea(Set<AreaInfo> subArea) {
            this.subArea = subArea;
        }
    }

    /**
     * 地址信息级别
     */
    public enum AreaLevel {
        /**
         * 根节点，没有任何意义
         */
        ROOT,
        /**
         * 国家
         */
        COUNTRY,
        /**
         * 省
         */
        STATE,
        /**
         * 市
         */
        CITY,
        /**
         * 区
         */
        REGION
    }

    /**
     * 给前台展示的地区信息
     */
    public static class AreaInfoVo {
        private String id;
        private AreaUtils.AreaLevel level;
        private String zonename;

        public AreaInfoVo(String id, AreaUtils.AreaLevel level, String zonename) {
            this.id = id;
            this.level = level;
            this.zonename = zonename;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public AreaUtils.AreaLevel getLevel() {
            return level;
        }

        public void setLevel(AreaUtils.AreaLevel level) {
            this.level = level;
        }

        public String getZonename() {
            return zonename;
        }

        public void setZonename(String zonename) {
            this.zonename = zonename;
        }
    }
}
