/*
 * Copyright 2013 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projog.website;

import static org.projog.website.BuiltInPredicatesIndexPage.produceBuiltInPredicatesIndexPage;
import static org.projog.website.WebsiteUtils.BUILTIN_PREDICATES_PACKAGE_DIR;
import static org.projog.website.WebsiteUtils.DOCS_OUTPUT_DIR;
import static org.projog.website.WebsiteUtils.EXTRACTED_OPERATOR_TESTS_DIR;
import static org.projog.website.WebsiteUtils.EXTRACTED_PREDICATE_TESTS_DIR;
import static org.projog.website.WebsiteUtils.FOOTER_HTML;
import static org.projog.website.WebsiteUtils.HEADER_HTML;
import static org.projog.website.WebsiteUtils.LINE_BREAK;
import static org.projog.website.WebsiteUtils.MANUAL_HTML;
import static org.projog.website.WebsiteUtils.SCRIPTS_OUTPUT_DIR;
import static org.projog.website.WebsiteUtils.SOURCE_INPUT_DIR_NAME;
import static org.projog.website.WebsiteUtils.STATIC_PAGES_LIST;
import static org.projog.website.WebsiteUtils.WEB_SRC_DIR;
import static org.projog.website.WebsiteUtils.readAllLines;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Produces all HTML pages that make up the web-site.
 * <p>
 * The {@code manual.html} page is generated by {@link #produceTableOfContents(List)}.
 * </p>
 * <p>
 * Designed to be run as a stand-alone single-threaded console application.
 * </p>
 */
public final class HtmlGenerator {
   private static final String FOOTER = readTextFile(new File(WEB_SRC_DIR, FOOTER_HTML)).toString();
   private static final String HEADER_BEFORE_TITLE;
   private static final String HEADER_AFTER_TITLE;
   static {
      final StringBuffer header = readTextFile(new File(WEB_SRC_DIR, HEADER_HTML));
      final int titlePos = header.indexOf("</title>");
      HEADER_BEFORE_TITLE = header.substring(0, titlePos);
      HEADER_AFTER_TITLE = header.substring(titlePos);
      DOCS_OUTPUT_DIR.mkdir();
   }
   private static String VERSION;

   public static void main(final String[] args) throws Exception {
      if (args.length != 1) {
         throw new IllegalArgumentException("expected single argument specifying the current version of the project");
      }
      VERSION = args[0];

      generateHtml();
   }

   /**
    * Generate all HTML pages that make up the web-site.
    */
   private static void generateHtml() throws Exception {
      produceWebContentNotIncludedInTableOfContents();

      CodeExamplesWebPageCreator hg = new CodeExamplesWebPageCreator();
      hg.generate(new File(SCRIPTS_OUTPUT_DIR, "concepts"));
      hg.generate(new File(SCRIPTS_OUTPUT_DIR, "applications"));
      List<CodeExampleWebPage> indexOfBuiltinPredicatePages = hg.generate(EXTRACTED_PREDICATE_TESTS_DIR);
      List<CodeExampleWebPage> indexOfBuiltinOperatorPages = hg.generate(EXTRACTED_OPERATOR_TESTS_DIR);

      TableOfContentsReader tocReader = new TableOfContentsReader(indexOfBuiltinPredicatePages, indexOfBuiltinOperatorPages, getBuiltinPredicatesPackageDescriptions());
      List<TableOfContentsEntry> entries = tocReader.getEntries();
      produceBuiltInPredicatesIndexPage();
      produceTableOfContents(entries);
      addPreviousAndNextLinks(entries);
   }

   /**
    * Produce pages that make up the web-site that are not included in the manual's "table of contents".
    * <p>
    * Adds standard HTML header and footer to all files specified in {@link WebsiteUtils#STATIC_PAGES_LIST}.
    */
   private static void produceWebContentNotIncludedInTableOfContents() throws Exception {
      Properties p = new Properties();
      try (FileInputStream fis = new FileInputStream(STATIC_PAGES_LIST)) {
         p.load(fis);
      }
      for (Map.Entry<Object, Object> entry : p.entrySet()) {
         String filename = (String) entry.getKey();
         String title = (String) entry.getValue();
         addHeadersAndFooters(title, new File(WEB_SRC_DIR, filename));
      }
   }

   private static Map<String, String> getBuiltinPredicatesPackageDescriptions() {
      HashMap<String, String> packageDescriptions = new HashMap<>();
      for (File dir : BUILTIN_PREDICATES_PACKAGE_DIR.listFiles()) {
         File packageInfo = new File(dir, "package-info.java");
         if (packageInfo.exists()) {
            String packageName = dir.getPath().substring(SOURCE_INPUT_DIR_NAME.length()).replace(File.separatorChar, '.');
            packageDescriptions.put(packageName, parsePackageInfo(packageInfo));
         }
      }
      return packageDescriptions;
   }

   /**
    * Parses javadoc from the specified {@code package-info.java} file.
    * <p>
    * Is very strict about the format of the javadoc. Expects:
    * <ol>
    * <li>First line to be the javadoc opening token.
    * <li>Second line to contain the description of the package (beginning with "Predicates" and ending with a
    * full-stop).
    * <li>Third line to be the javadoc closing token.
    * <ol>
    */
   private static String parsePackageInfo(File packageInfo) {
      try {
         List<String> lines = readAllLines(packageInfo);
         String firstLine = lines.get(0).trim();
         String secondLine = lines.get(1).trim();
         String thirdLine = lines.get(2).trim();

         if (!"/**".equals(firstLine)) {
            throw new RuntimeException(firstLine);
         }
         if (!secondLine.startsWith("* Predicates") || !secondLine.endsWith(".")) {
            throw new RuntimeException(secondLine);
         }
         if (!"*/".equals(thirdLine)) {
            throw new RuntimeException(thirdLine);
         }

         // strip leading '* ' and replace trailing '.' with ':'
         return secondLine.substring(2, secondLine.length() - 1) + ':';
      } catch (Exception e) {
         throw new RuntimeException("Exception reading: " + packageInfo, e);
      }
   }

   /**
    * Produces the manual's "table of contents" page ({@link WebsiteUtils#MANUAL_HTML}).
    */
   private static void produceTableOfContents(List<TableOfContentsEntry> entries) throws Exception {
      File manualHtml = new File(DOCS_OUTPUT_DIR, MANUAL_HTML);

      try (FileWriter fw = new FileWriter(manualHtml); BufferedWriter bw = new BufferedWriter(fw)) {
         bw.write("<span class=\"manual\">");
         for (TableOfContentsEntry next : entries) {
            printTableOfContentsEntry(bw, next);
         }
         bw.write("</span>");
      } catch (Exception e) {
         e.printStackTrace();
         throw e;
      }

      addHeadersAndFooters("Manual Contents", manualHtml);
   }

   private static void printTableOfContentsEntry(BufferedWriter bw, TableOfContentsEntry next) throws IOException {
      if (next.isHeader()) {
         bw.write(next.getIndex() + " " + next.getTitle() + "<br>" + LINE_BREAK);
      } else if (next.isDescription()) {
         bw.write("&nbsp;<i>" + next.getTitle() + "</i><br>" + LINE_BREAK);
      } else {
         if (next.isSubSection()) {
            bw.write("&nbsp;&nbsp;&nbsp; ");
         }
         bw.write(next.getIndex());
         bw.write(" <a href=\"" + next.getFileName() + "\">" + next.getTitle() + "</a><br>" + LINE_BREAK);

         File sourceFile = new File(WEB_SRC_DIR, next.getFileName());
         if (!sourceFile.exists()) {
            sourceFile = new File(DOCS_OUTPUT_DIR, next.getFileName());
         }
      }
   }

   private static void addPreviousAndNextLinks(List<TableOfContentsEntry> entries) {
      for (TableOfContentsEntry entry : entries) {
         if (entry.isLink()) {
            addPreviousAndNextLinks(entry);
         }
      }
   }

   private static void addPreviousAndNextLinks(TableOfContentsEntry entry) {
      StringBuffer linksHtml = new StringBuffer();
      linksHtml.append(LINE_BREAK);
      linksHtml.append("<div class=\"full-width\"><div class=\"previous\">");
      outputLink(entry.getPrevious(), linksHtml);
      linksHtml.append("</div><div class=\"next\">");
      outputLink(entry.getNext(), linksHtml);
      linksHtml.append("</div><div style=\"clear: both;\"></div></div>");
      linksHtml.append(LINE_BREAK);

      File sourceFile = getFile(entry);
      StringBuffer contents = readTextFile(sourceFile);
      contents.insert(0, "<hr><h2>" + entry.getIndex() + " " + entry.getTitle() + "</h2>" + LINE_BREAK + "<div class=\"section-content\">" + LINE_BREAK);
      contents.insert(0, linksHtml);
      contents.append("</div><hr>");
      contents.append(linksHtml);
      addHeadersAndFooters(entry.getFileName(), entry.getTitle(), contents);
   }

   private static File getFile(TableOfContentsEntry entry) {
      File f = new File(WEB_SRC_DIR, entry.getFileName());
      if (!f.exists()) {
         f = new File(DOCS_OUTPUT_DIR, entry.getFileName());
      }
      return f;
   }

   private static void outputLink(TableOfContentsEntry entry, StringBuffer sb) {
      if (entry != null) {
         sb.append("<a href=\"");
         sb.append(entry.getFileName());
         sb.append("\">");
         sb.append(entry.getIndex());
         sb.append(" ");
         sb.append(entry.getTitle());
         sb.append("</a>");
         sb.append(LINE_BREAK);
      }
   }

   private static void addHeadersAndFooters(String title, File input) {
      addHeadersAndFooters(input.getName(), title, readTextFile(input));
   }

   private static void addHeadersAndFooters(String filename, String title, CharSequence content) {
      String html = compactHtml(HEADER_BEFORE_TITLE + removeHtmlMarkup(title) + HEADER_AFTER_TITLE + tokenFilter(content) + FOOTER);

      try (FileWriter fw = new FileWriter(new File(DOCS_OUTPUT_DIR, filename))) {
         fw.write(html.trim());
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /** unsophisticated attempt at compacting html by removing unnecessary whitespace */
   private static String compactHtml(String inputHtml) {
      String output = inputHtml.replaceAll("\\s+", " ").trim();

      output = output.replace(" <br>", "<br>").replace("<br> ", "<br>");

      for (final String tag : new String[] {"body", "html", "head", "title", "meta", "p", "div", "ul", "ol", "li", "h3", "h2", "h1", "h4", "img", "center"}) {
         output = output.replace(" <" + tag + " ", "<" + tag + " ");
         output = output.replace("<" + tag + "> ", "<" + tag + ">");
         output = output.replace(" <" + tag + ">", "<" + tag + ">");
         output = output.replace("</" + tag + "> ", "</" + tag + ">");
         output = output.replace(" </" + tag + ">", "</" + tag + ">");
      }

      if (output.contains("<pre") || output.contains("<script")) {
         // this approach could alter the behaviour of <pre> and <script> tags, so throw an exception if the content contains them
         throw new RuntimeException(inputHtml);
      }

      return output;
   }

   private static String tokenFilter(CharSequence content) {
      return content.toString().replace("@PROJOG_VERSION@", VERSION);
   }

   private static String removeHtmlMarkup(String input) {
      return input.replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "").replace("<code>", "").replace("</code>", "");
   }

   private static StringBuffer readTextFile(File f) {
      StringBuffer contents = new StringBuffer();
      try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr)) {
         String next;
         while ((next = br.readLine()) != null) {
            contents.append(next + LINE_BREAK);
         }
         return contents;
      } catch (Exception e) {
         System.out.println("CANNOT READ: " + f.getAbsolutePath());
         throw new RuntimeException(e);
      }
   }
}
