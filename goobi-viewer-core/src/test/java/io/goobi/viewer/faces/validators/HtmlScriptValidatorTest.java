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
package io.goobi.viewer.faces.validators;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.faces.validators.HtmlScriptValidator;

/**
 * @author Florian Alpers
 *
 */
public class HtmlScriptValidatorTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        Assert.assertTrue("Does not accept <p> tag", new HtmlScriptValidator().validate("abc\njkl  h <p>asdasd</p> ashdoha<br/> asdas"));
        Assert.assertTrue("Does not accept <div> tag with attribute",
                new HtmlScriptValidator().validate("abc\njkl  h <div test=\"asd\">asdasd</div> ashdoha<br/> asdas"));
        Assert.assertTrue("Does not accept text with em and br",
                new HtmlScriptValidator().validate("abc\njkl  h <em>asdasd</em> ashdoha<br/> asdas"));
        Assert.assertTrue("Does not accept text with em with attribute",
                new HtmlScriptValidator().validate("abc\njkl  h <em test=\"asd\">asdasd</em> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts text with script tag",
                new HtmlScriptValidator().validate("abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts <script> tag in html body", new HtmlScriptValidator()
                .validate("<head><p>asdas</p></head> <body>abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas</body>"));
        Assert.assertTrue("Does not accept <body> tag with <div>",
                new HtmlScriptValidator().validate("<body>abc\njkl  h <div type=\"hidden\">asdasd</div> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts <body> tag with <script>", new HtmlScriptValidator()
                .validate("<head></head><body>abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas</body>"));
        Assert.assertFalse("Accepts <script> tag in html",
                new HtmlScriptValidator().validate("<html><script>var i = 1;</script><head></head><body>asdas</body></html>"));
        Assert.assertFalse("Accepts pure <script> tag", new HtmlScriptValidator().validate("<script>var i = 1;</script>"));

    }

}
