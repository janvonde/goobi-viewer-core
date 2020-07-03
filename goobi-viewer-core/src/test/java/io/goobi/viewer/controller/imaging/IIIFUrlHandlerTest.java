/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller.imaging;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author Florian Alpers
 *
 */
public class IIIFUrlHandlerTest extends AbstractTest {

    IIIFUrlHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
        handler = new IIIFUrlHandler();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.controller.imaging.IIIFUrlHandler#getIIIFImageUrl(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)}.
     * 
     * @throws ConfigurationException
     */
    @Test
    public void testGetIIIFImageUrl() {

        String fileUrl = "filename.tif";
        String pi = "1234";
        String region = "full";
        String size = "max";
        String rotation = "0";
        String quality = "default";
        String format = "jpg";

        String url;

        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "rest/records/1234/files/images/filename.tif/full/max/0/default.jpg", url);

        fileUrl = "http://localhost/image/filename.tif";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals(
                ConfigurationTest.APPLICATION_ROOT_URL + "rest/image/-/http:U002FU002FlocalhostU002FimageU002Ffilename.tif/full/max/0/default.jpg",
                url);

        fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/full/0/native.jpg";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals("http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/max/0/default.jpg", url);

        fileUrl = "file:///image/filename.tif";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "rest/image/-/file:U002FU002FU002FimageU002Ffilename.tif/full/max/0/default.jpg",
                url);

        fileUrl = "file:///image/filename 01.tif";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals(
                ConfigurationTest.APPLICATION_ROOT_URL + "rest/image/-/file:U002FU002FU002FimageU002Ffilename%2001.tif/full/max/0/default.jpg", url);

        // TODO
        //        fileUrl = "/image/filename.tif";
        //        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        //        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "rest/image/-/file:U002FimageU002Ffilename.tif/full/max/0/default.jpg", url);

        fileUrl = "http://localhost/image/filename.tif";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals(
                ConfigurationTest.APPLICATION_ROOT_URL + "rest/image/-/http:U002FU002FlocalhostU002FimageU002Ffilename.tif/full/max/0/default.jpg",
                url);

        fileUrl = "file:///C:/opt/digiverso/viewer/cms_media/filename.tif";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format);
        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL
                + "rest/image/-/file:U002FU002FU002FC:U002FoptU002FdigiversoU002FviewerU002Fcms_mediaU002Ffilename.tif/full/max/0/default.jpg", url);

    }

}
