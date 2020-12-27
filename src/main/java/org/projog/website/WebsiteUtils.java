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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.projog.api.Projog;
import org.projog.core.parser.Operands;
import org.projog.core.parser.SentenceParser;
import org.projog.core.predicate.PredicateKey;
import org.projog.core.term.Term;

/** Constants and utility methods used in the build process. */
final class WebsiteUtils {
   static final PredicateKey ADD_PREDICATE_KEY = new PredicateKey("pj_add_predicate", 2);
   static final PredicateKey ADD_CALCULATABLE_KEY = new PredicateKey("pj_add_calculatable", 2);
   static final String BUILTIN_PREDICATES_PACKAGE_NAME = "org.projog.core.predicate.builtin";
   static final String BUILTIN_OPERATORS_PACKAGE_NAME = "org.projog.core.math.builtin";
   private static final File BUILD_DIR = new File("build");
   static final File WEB_SRC_DIR = new File("web");
   static final File DOCS_OUTPUT_DIR = new File(BUILD_DIR, "docs");
   static final File BOOTSTRAP_FILE = new File("../projog/src/main/resources/projog-bootstrap.pl");
   static final File EXTRACTED_PREDICATE_TESTS_DIR = new File("../projog/target/prolog-predicate-tests-extracted-from-java");
   static final File EXTRACTED_OPERATOR_TESTS_DIR = new File("../projog/target/prolog-operator-tests-extracted-from-java");
   static final File SCRIPTS_OUTPUT_DIR = new File("../projog/src/test/prolog");
   static final File MANUAL_TEMPLATE = new File(WEB_SRC_DIR, "manual.txt");
   static final File STATIC_PAGES_LIST = new File(WEB_SRC_DIR, "static_pages.properties");
   static final File COMMANDS_INDEX_FILE = new File(DOCS_OUTPUT_DIR, "prolog-predicates.html");
   static final String SOURCE_INPUT_DIR_NAME = "../projog/src/main/java/";
   static final File SOURCE_INPUT_DIR = new File(SOURCE_INPUT_DIR_NAME);
   static final File BUILTIN_PREDICATES_PACKAGE_DIR = new File(SOURCE_INPUT_DIR, BUILTIN_PREDICATES_PACKAGE_NAME.replace('.', File.separatorChar));
   static final String LINE_BREAK = "\n";
   static final String HTML_FILE_EXTENSION = ".html";
   private static final String PROLOG_FILE_EXTENSION = ".pl";
   static final String TEXT_FILE_EXTENSION = ".txt";
   static final String MANUAL_HTML = "manual" + HTML_FILE_EXTENSION;
   static final String HEADER_HTML = "header" + HTML_FILE_EXTENSION;
   static final String FOOTER_HTML = "footer" + HTML_FILE_EXTENSION;
   /** only used by unit-tests */
   static final File TEST_RESOURCES_DIR = new File("src/test/resources");

   private WebsiteUtils() {
   }

   /** Returns {@code true} if the the specified file has a prolog file extension. */
   static boolean isPrologScript(File f) {
      return f.getName().endsWith(PROLOG_FILE_EXTENSION);
   }

   /**
    * Returns list of lines contained in specified text file.
    *
    * @param f text file to read
    * @return list of lines contained in specified file
    */
   static List<String> readAllLines(File f) {
      try {
         return Files.readAllLines(f.toPath(), Charset.defaultCharset());
      } catch (Exception e) {
         throw new RuntimeException("could not read text file: " + f, e);
      }
   }

   static String htmlEncode(String input) {
      return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("  ", "&nbsp;&nbsp;").replace(LINE_BREAK, "<br>" + LINE_BREAK);
   }

   static Term[] parseTermsFromFile(File f) {
      Operands operands = new Projog().getKnowledgeBase().getOperands();
      try (FileReader fr = new FileReader(f)) {
         SentenceParser sp = SentenceParser.getInstance(fr, operands);

         ArrayList<Term> result = new ArrayList<>();
         Term next;
         while ((next = sp.parseSentence()) != null) {
            result.add(next);
         }
         return result.toArray(new Term[result.size()]);
      } catch (IOException e) {
         throw new RuntimeException("Could not parse: " + f, e);
      }
   }
}
