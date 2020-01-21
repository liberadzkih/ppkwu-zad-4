package com.ppkwu.zad4;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;


/*
    Proszę przygotować dokumentację i zaimplementować (wg tej dokumentacji) API,
    które będzie generowało stronę mobilną wykorzystującą wyszukiwarkę pracowników ze strony https://www.p.lodz.pl/pl.
    Po wyszukaniu wyników (może ich być więcej, niż jeden!!!).
    Powinna być możliwość kliknięcia na opcję "wygeneruj vCard" dla każdego znalezionego wpisu.
    Następnie proszę "podpiąć" taką stronę pod swój telefon i sprawdzić, czy działa i czy wygenerowane wizytówki można dodać do kontaktów.
    Na maksymalną ocenę konieczne jest zaimplementowanie logowania (bez tego maile i numery telefonów są niewidoczne) i "wyciągania" maili i numerów telefonów (np. dla "Nowak") dla wyników wyszukiwania z p.lodz.pl.

    Proszę załączyć całość projektu spakowanego zipem.
    Na samej górze pola tekstowego powinien się znajdować link do DOKUMENTACJI tego konkretnego projektu z Państwa githuba,
    a  (oddzielony SHIFT+ENTEREM) niżej link do pliku zawierającego "główny" kod źródłowy.
    W trzeciej linijce ma się znajdować link do wygenerowanego vcarda dla Pawła Kapusty.
    Wklejone linki mają być "klikalne", w przeciwnym razie zadanie nie zostanie sprawdzone.

    Proszę dopisać, jeśli nie zrobili Państwo logowania, czyli wyświetlania maila i telefonu.
 */
@RestController
public class VCard {

    @RequestMapping("VCard")
    public String startPageVCard() throws IOException {
        String filePath = "first_page.html";
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    @RequestMapping("VCard/{name}")
    public String getVCard(@PathVariable String name) throws IOException {

        String url = buildHttpPath(name);
        HashMap<String, List<String>> hashMap =  makeHashMap(url);
        List<String> pictures = hashMap.get("pictures");
        List<String> names = hashMap.get("names");
        List<String> titles = hashMap.get("titles");
        List<String> extraInfo = hashMap.get("info");
        int listSize = pictures.size();
        List<String> VCard = new ArrayList<>();

        for (int i = 0; i < listSize; i++) {
            VCard.add("<center>");
            VCard.add("<br>");
            VCard.add("<img src='" + pictures.get(i) + "'>");
            VCard.add("<br>");
            VCard.add(titles.get(i) + " " + names.get(i) + "<br>" + extraInfo.get(i));
            VCard.add("<form action='http://localhost:8080/VCard/" + name + "/"+ i + "' />");
            VCard.add("     <input id='" + i + "' type='submit' value='Wygeneruj VCard' />");
            VCard.add("</form>");
            VCard.add("</center>");
            VCard.add("<hr>");
        }

        return VCard.toString().replace("[", "").replace("]", "").replace(",", "");
    }

    @RequestMapping("VCard/{name}/{number}")
    public void generateVCard(@PathVariable String name, @PathVariable int number, HttpServletResponse response) throws IOException {
        String url = buildHttpPath(name);
        HashMap<String, List<String>> hashMap =  makeHashMap(url);
        List<String> names = hashMap.get("names");
        List<String> titles = hashMap.get("titles");
        List<String> extraInfo = hashMap.get("info");
        try {
            File file = generateCard(names.get(number), titles.get(number), extraInfo.get(number));
            String filePathToBeServed = file.getAbsolutePath();
            InputStream inputStream = new FileInputStream(file);
            response.setContentType("application/force-download");
            response.setHeader("Content-Disposition", "attachment; filename="+file.getName());
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
            inputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public File generateCard(String name, String title, String info) throws IOException {

        String firstname = name.substring(0, name.indexOf(' '));
        String surname = name.substring(name.indexOf(' ')+1);
        File file = new File(firstname + "_" + surname + "_vCard" + ".vcf");
        file = file.getAbsoluteFile();
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("BEGIN:VCARD\r\n");
        bufferedWriter.write("VERSION:4.0\r\n");
        bufferedWriter.write("N:" + surname + ";" + firstname + ";;" + title + "\r\n");
        bufferedWriter.write("FN:" + firstname + " " + surname + "\r\n");
        bufferedWriter.write("ORG:" + info + "\r\n");
        bufferedWriter.write("END:VCARD");
        bufferedWriter.close();
        return file;
    }

    public String buildHttpPath(String name){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://adm.edu.p.lodz.pl/").append("user/users.php?search=");
        stringBuilder.append(name);
        return stringBuilder.toString();
    }

    public HashMap<String, List<String>> makeHashMap(String url) throws IOException {
        Document document;
        document = Jsoup.parse(new URL(url), 10000);

        Elements html = document.select("div.userlist");
        Elements userPicture = html.select("img.userpicture");
        Elements userDetails = html.select("div.user-info");
        Elements userName = userDetails.select("h3");
        Elements userAcademicTitle = userDetails.select("h4");
        Elements userExtraInfo = userDetails.select("div.extra-info");
        Elements userPublicProfile = userDetails.select("a.fullprofile-link");


        List<String> pictures = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<String> extraInfo = new ArrayList<>();
        List<String> publicProfiles = new ArrayList<>();

        for (Element e : userPicture)
            pictures.add(e.attr("src"));
        for (Element e : userName)
            names.add(e.text());
        for (Element e : userAcademicTitle)
            titles.add(e.text());
        for (Element e : userExtraInfo)
            extraInfo.add(e.text());
        for (int i=0; i<userPublicProfile.size(); i++)
            if (i % 2 == 0)
                publicProfiles.add(userPublicProfile.get(i).attr("href"));

        HashMap<String, List<String>>  lists = new HashMap<>();
        lists.put("pictures", pictures);
        lists.put("names", names);
        lists.put("titles", titles);
        lists.put("info", extraInfo);
        lists.put("profiles", publicProfiles);
        return lists;
    }

}
