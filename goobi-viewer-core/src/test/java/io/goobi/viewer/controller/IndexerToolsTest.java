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
package io.goobi.viewer.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class IndexerToolsTest extends AbstractTest {

    /**
     * @see IndexerTools#deleteRecord(String,boolean,Path)
     * @verifies create delete file correctly
     */
    @Test
    public void deleteRecord_shouldCreateDeleteFileCorrectly() throws Exception {
        Path hotfolder = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder());
        if (!Files.isDirectory(hotfolder)) {
            Files.createDirectory(hotfolder);
        }
        Path file = Paths.get(hotfolder.toAbsolutePath().toString(), "PPN123.delete");
        try {
            Assert.assertTrue(IndexerTools.deleteRecord("PPN123", true, hotfolder));
            Assert.assertTrue(Files.isRegularFile(file));
        } finally {
            if (Files.isRegularFile(file)) {
                Files.delete(file);
            }
            if (!Files.isDirectory(hotfolder)) {
                Files.delete(hotfolder);
            }
        }
    }

    /**
     * @see IndexerTools#deleteRecord(String,boolean,Path)
     * @verifies create purge file correctly
     */
    @Test
    public void deleteRecord_shouldCreatePurgeFileCorrectly() throws Exception {
        Path hotfolder = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder());
        if (!Files.isDirectory(hotfolder)) {
            Files.createDirectory(hotfolder);
        }
        Path file = Paths.get(hotfolder.toAbsolutePath().toString(), "PPN123.purge");
        try {
            Assert.assertTrue(IndexerTools.deleteRecord("PPN123", false, hotfolder));
            Assert.assertTrue(Files.isRegularFile(file));
        } finally {
            if (Files.isRegularFile(file)) {
                Files.delete(file);
            }
            if (!Files.isDirectory(hotfolder)) {
                Files.delete(hotfolder);
            }
        }
    }

    /**
     * @see IndexerTools#findNamingScheme(String,String,File[])
     * @verifies return original baseName if no files exist
     */
    @Test
    public void findNamingScheme_shouldReturnOriginalBaseNameIfNoFilesExist() throws Exception {
        Path hotfolder = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder());
        try {
            if (!Files.exists(hotfolder)) {
                Files.createDirectory(hotfolder);
            }
            Assert.assertTrue(Files.isDirectory(hotfolder));

            Assert.assertEquals("foo", IndexerTools.findNamingScheme("foo", "xml"));
        } finally {
            FileUtils.deleteDirectory(hotfolder.toFile());
        }
    }

    /**
     * @see IndexerTools#findNamingScheme(String,String,File[])
     * @verifies return alternative naming scheme if initial name already exists
     */
    @Test
    public void findNamingScheme_shouldReturnAlternativeNamingSchemeIfInitialNameAlreadyExists() throws Exception {
        Path hotfolder = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder());
        try {
            if (!Files.exists(hotfolder)) {
                Files.createDirectory(hotfolder);
            }
            Assert.assertTrue(Files.isDirectory(hotfolder));

            Path dataFolder = Paths.get(hotfolder.toAbsolutePath().toString(), "foo_data");
            Files.createDirectory(dataFolder);
            Assert.assertTrue(Files.isDirectory(dataFolder));

            Assert.assertTrue(IndexerTools.findNamingScheme("foo", "xml", dataFolder.toFile()).startsWith("foo#"));
        } finally {
            FileUtils.deleteDirectory(hotfolder.toFile());
        }
    }
}
