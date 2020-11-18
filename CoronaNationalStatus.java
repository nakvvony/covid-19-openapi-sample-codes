package com.api.corona.national;
/*
    @title 공공데이터포털_보건복지부_코로나_해외발생현황_API_사용_샘플코드
    @author 윤낙원
    @date 2020-11-19
 */


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class NationInfo {
    private String areaNm;
    private String areaNmEn;
    private String nationNm;
    private String nationNmEn;
    private long natDefCnt;
    private long natDeathCnt;
    private double natDeathRate;
    private String createDt;

    public String getAreaNm() {
        return areaNm;
    }

    public void setAreaNm(String areaNm) {
        this.areaNm = areaNm;
    }

    public String getAreaNmEn() {
        return areaNmEn;
    }

    public void setAreaNmEn(String areaNmEn) {
        this.areaNmEn = areaNmEn;
    }

    public String getNationNm() {
        return nationNm;
    }

    public void setNationNm(String nationNm) {
        this.nationNm = nationNm;
    }

    public String getNationNmEn() {
        return nationNmEn;
    }

    public void setNationNmEn(String nationNmEn) {
        this.nationNmEn = nationNmEn;
    }

    public long getNatDefCnt() {
        return natDefCnt;
    }

    public void setNatDefCnt(String natDefCnt) {
        this.natDefCnt = Long.parseLong(natDefCnt);
    }

    public long getNatDeathCnt() {
        return natDeathCnt;
    }

    public void setNatDeathCnt(String natDeathCnt) {
        this.natDeathCnt = Long.parseLong(natDeathCnt);
    }

    public double getNatDeathRate() {
        return natDeathRate;
    }

    public void setNatDeathRate(String natDeathRate) {
        this.natDeathRate = Double.parseDouble(natDeathRate);
    }

    public String getCreateDt() {
        return createDt;
    }

    public void setCreateDt(String createDt) {
        this.createDt = createDt;
    }

}

class CoronaNationalStatus {
    static String urlBuilder;
    static String UTF = "UTF-8";
    static String SERVICE_URL = "http://openapi.data.go.kr/openapi/service/rest/Covid19/" +
            "getCovid19NatInfStateJson";
    static String SERVICE_KEY = "="; //보건복지부_코로나19해외발생_현황 일반 인증키(UTF-8)
    static DecimalFormat formatter;
    static String standardDate = "-";

    static SimpleDateFormat dateFormatForComp, dateFormat_year, dateFormat_month, dateFormat_day, dateFormat_hour;
    static Date time;
    static String sYear, sMonth, sDay, sHour, sToday, sYesterday, sTwoDayAgo;
    static String stdYestFromServer, stdTodayFromServer;  //서버에 가장 최근에 데이터가 등록된 날짜

    static int nYear, nMonth, nDay, nHour;
    static int[] days = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static int todayTotalNatDefCnt, todayTotalNatDeathCnt, yestTotalDefCnt, yestNatDeathCnt;

    static Element body, items, item;
    static Node areaNm, areaNmEn, nationNm, nationNmEn, natDefCnt, natDeathCnt, natDeathRate, createDt, stdDt;
    static ArrayList<NationInfo> natInfoList = new ArrayList<>();

    static void init() {
        dateFormatForComp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat_year = new SimpleDateFormat("yyyy", Locale.getDefault());
        dateFormat_month = new SimpleDateFormat("MM", Locale.getDefault());
        dateFormat_day = new SimpleDateFormat("dd", Locale.getDefault());
        dateFormat_hour = new SimpleDateFormat("HH", Locale.getDefault());
        time = new Date();

        formatter = new DecimalFormat("###,###");

        sYear = dateFormat_year.format(time);
        sMonth = dateFormat_month.format(time);
        sDay = dateFormat_day.format(time);
        sHour = dateFormat_hour.format(time);

        nYear = Integer.parseInt(sYear);
        nMonth = Integer.parseInt(sMonth);
        nDay = Integer.parseInt(sDay);
        nHour = Integer.parseInt(sHour);

        sToday = dayAgo(0);
        sToday = sToday.substring(0, 4) + '-'
                + sToday.substring(4, 6) + '-' + sToday.substring(6, 8);
        sYesterday = dayAgo(1);
        sYesterday = sYesterday.substring(0, 4) + '-'
                + sYesterday.substring(4, 6) + '-' + sYesterday.substring(6, 8);
        sTwoDayAgo = dayAgo(2);
        sTwoDayAgo = sTwoDayAgo.substring(0, 4) + '-'
                + sTwoDayAgo.substring(4, 6) + '-' + sTwoDayAgo.substring(6, 8);
    }

    public static String dayAgo(int subNum) {
        return calDate(nYear, nMonth, nDay, subNum);
    }

    private static String calDate(int year, int month, int day, int subNumber) {   //n일 전의 date 반환하는 함수
        String date;

        if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) {    //윤년 계산
            days[1] = 29;
        } else {
            days[1] = 28;
        }

        if (subNumber >= day) {
            if (month != 1) {
                day += days[month - 1];
                day -= subNumber;
                month--;
            } else {
                day += days[12];
                day -= subNumber;
                month = 12;
                year--;
            }
        } else {
            day -= subNumber;
        }

        date = Integer.toString(year);

        if (month < 10) {
            date += "0" + month;
        } else {
            date += month;
        }

        if (day < 10) {
            date += "0" + day;
        } else {
            date += day;
        }

        return date;
    }

    CoronaNationalStatus(DecimalFormat formatter) {
        CoronaNationalStatus.formatter = formatter;
    }

    private static void loadXML() {
        int nYesterday = 1,
                nToday = 0;
        for (int i = 0; i < 2; i++) {
            try {
                urlBuilder = SERVICE_URL + "?" + URLEncoder.encode("ServiceKey", UTF) + SERVICE_KEY + /*Service Key*/
                        "&" + URLEncoder.encode("pageNo", UTF) + "=" + URLEncoder.encode("1", UTF) + /*페이지번호*/
                        "&" + URLEncoder.encode("numOfRows", UTF) + "=" + URLEncoder.encode("10", UTF) + /*한 페이지 결과 수*/
                        "&" + URLEncoder.encode("startCreateDt", UTF) + "=" + URLEncoder.encode(dayAgo(nYesterday), UTF) + /*검색할 생성일 범위의 시작*/
                        "&" + URLEncoder.encode("endCreateDt", UTF) + "=" + URLEncoder.encode(dayAgo(nToday), UTF);/*URL*//*검색할 생성일 범위의 종료*/
                if (i == 1) {
                    System.out.println("INFO_URL - URL:" + urlBuilder);
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }

            Document doc = null;
            try {
                URL url = new URL(urlBuilder);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(new InputSource(url.openStream()));
                doc.getDocumentElement().normalize();
            } catch (IOException | SAXException | ParserConfigurationException e) {
                System.out.println("CoronaNationalStatus()" + e.getMessage());
            }

            assert doc != null;
            body = (Element) doc.getElementsByTagName("body").item(0);
            items = (Element) body.getElementsByTagName("items").item(0);
            item = (Element) items.getElementsByTagName("item").item(0);

            Node tmpCreateDt = item.getElementsByTagName("createDt").item(0);
            String sTmpCreateDt = tmpCreateDt.getChildNodes().item(0).getNodeValue();
            if (i == 0) {
                if (!sTmpCreateDt.substring(0, 10).equals(sToday)) {
                    System.out.println(sTmpCreateDt.substring(0, 10) + "-" + sToday);
                    nYesterday = 2;
                    nToday = 1;
                    stdYestFromServer = sTwoDayAgo;
                    stdTodayFromServer = sYesterday;
                } else {
                    stdYestFromServer = sYesterday;
                    stdTodayFromServer = sToday;
                    break;
                }
            }
        }
    }

    private static void XMLParse() {
        loadXML();
        System.out.println("서버기준 오늘: " + stdTodayFromServer);
        System.out.println("서버기준 어제: " + stdYestFromServer);
        int i = 0;
        while (true) {
            NationInfo nationInfo = new NationInfo();
            item = (Element) items.getElementsByTagName("item").item(i);

            if (item == null) {
                break;
            }

            if (i++ == 0) {
                stdDt = item.getElementsByTagName("stdDay").item(0);
                standardDate = stdDt.getChildNodes().item(0).getNodeValue();
            }

            areaNm = item.getElementsByTagName("areaNm").item(0);    //지역명
            areaNmEn = item.getElementsByTagName("areaNmEn").item(0);    //지역명(영문)
            nationNm = item.getElementsByTagName("nationNm").item(0);    //국가명
            nationNmEn = item.getElementsByTagName("nationNmEn").item(0);    //국가명(영문)
            natDefCnt = item.getElementsByTagName("natDefCnt").item(0);    //확진자 수(국가별)
            natDeathCnt = item.getElementsByTagName("natDeathCnt").item(0);    //사망자 수(국가별)
            natDeathRate = item.getElementsByTagName("natDeathRate").item(0);    //확진자 대비 사망률
            createDt = item.getElementsByTagName("createDt").item(0);       //등록일자

            nationInfo.setAreaNm(areaNm.getChildNodes().item(0).getNodeValue());
            nationInfo.setAreaNmEn(areaNmEn.getChildNodes().item(0).getNodeValue());
            nationInfo.setNationNm(nationNm.getChildNodes().item(0).getNodeValue());
            nationInfo.setNationNmEn(nationNmEn.getChildNodes().item(0).getNodeValue());
            nationInfo.setNatDefCnt(natDefCnt.getChildNodes().item(0).getNodeValue());
            nationInfo.setNatDeathCnt(natDeathCnt.getChildNodes().item(0).getNodeValue());
            nationInfo.setNatDeathRate(natDeathRate.getChildNodes().item(0).getNodeValue());
            nationInfo.setCreateDt(createDt.getChildNodes().item(0).getNodeValue());

            natInfoList.add(nationInfo);
        }

        int n = 0;
        int todayCnt = 0;
        todayTotalNatDefCnt = todayTotalNatDeathCnt = 0;
        for (NationInfo natInfo : natInfoList) {
            System.out.println("----------------------------------------");
            System.out.println("#" + ++n);
            System.out.println("지역명: " + natInfo.getAreaNm());
            System.out.println("지역명_영문: " + natInfo.getAreaNmEn());
            System.out.println("국가명: " + natInfo.getNationNm());
            System.out.println("국가명_영문: " + natInfo.getNationNmEn());
            System.out.println("확진자 수: " + formatter.format(natInfo.getNatDefCnt()) + "명");
            System.out.println("사망자 수: " + formatter.format(natInfo.getNatDeathCnt()) + "명");
            System.out.println("확진자 대비 사망률: " + Math.round(natInfo.getNatDeathRate() * 100) / 100.00 + "%");
            System.out.println("등록일자: " + natInfo.getCreateDt().substring(0, 19));

            if (stdTodayFromServer.equals(natInfo.getCreateDt().substring(0, 10))) {
                todayTotalNatDefCnt += natInfo.getNatDefCnt();
                todayTotalNatDeathCnt += natInfo.getNatDeathCnt();
                todayCnt++;
            }
            if (stdYestFromServer.equals(natInfo.getCreateDt().substring(0, 10))) {
                yestTotalDefCnt += natInfo.getNatDefCnt();
                yestNatDeathCnt += natInfo.getNatDeathCnt();
            }
        }

        String newStandardDate = standardDate.substring(0, 4) + '-' + standardDate.substring(6, 8)
                + '-' + standardDate.substring(10, 12) + ' ' + standardDate.substring(14, 16) + "시";
        System.out.println("----------------------------------------");
        System.out.println("[ 정리 ]");
        System.out.println("기준일시: " + newStandardDate);
        System.out.println("현재 시간: " + nHour + "시(24시기준)");
        System.out.println("총 확진자 수: " + formatter.format(todayTotalNatDefCnt) + "명");
        System.out.println("총 사망자 수: " + formatter.format(todayTotalNatDeathCnt) + "명");
        System.out.println("총 확진자 증가 수(전일대비 기준): " + formatter.format(
                (todayTotalNatDefCnt - yestTotalDefCnt)) + "명");
        System.out.println("총 사망자 증가 수(전일대비 기준): " + formatter.format(
                (todayTotalNatDeathCnt - yestNatDeathCnt)) + "명");
        System.out.println("감염국가 수: " + todayCnt);
    }

    public static void main(String[] args) {
        init();
        XMLParse();
    }
}

