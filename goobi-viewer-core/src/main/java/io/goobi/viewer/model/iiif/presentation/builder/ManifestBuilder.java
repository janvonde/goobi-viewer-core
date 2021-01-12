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
package io.goobi.viewer.model.iiif.presentation.builder;

import java.net.URI;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.search.AutoSuggestService;
import de.intranda.api.iiif.search.SearchService;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * ManifestBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ManifestBuilder.class);
    protected ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();
    private BuildMode buildMode = BuildMode.IIIF;

    /**
     * <p>
     * Constructor for ManifestBuilder.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public ManifestBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }
    

    /**
     * <p>
     * generateManifest.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.api.iiif.presentation.IPresentationModelElement} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public AbstractPresentationModelElement generateManifest(StructElement ele)
            throws URISyntaxException, PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {

        final AbstractPresentationModelElement manifest;

        if (ele.isAnchor()) {
            manifest = new Collection(getManifestURI(ele.getPi()), ele.getPi());
            manifest.addViewingHint(ViewingHint.multipart);
        } else {
            manifest = new Manifest(getManifestURI(ele.getPi()));
            SearchService search = new SearchService(getSearchServiceURI(manifest.getId()));
            search.setLabel(ViewerResourceBundle.getTranslations("label__iiif_api_search"));
            AutoSuggestService autoComplete = new AutoSuggestService(getAutoSuggestServiceURI(manifest.getId()));
            search.addService(autoComplete);
            manifest.addService(search);
        }

        populate(ele, manifest);

        return manifest;
    }

    /**
     * <p>
     * populate.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public void populate(StructElement ele, final AbstractPresentationModelElement manifest)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException {
        this.getAttributions().forEach(attr -> manifest.addAttribution(attr));
        manifest.setLabel(new SimpleMetadataValue(ele.getLabel()));
        getDescription(ele).ifPresent(desc -> manifest.setDescription(desc));

        addMetadata(manifest, ele);

        try {
            String thumbUrl = imageDelivery.getThumbs().getThumbnailUrl(ele);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl));
                manifest.addThumbnail(thumb);
                if (IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {
                    String imageInfoURI = IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl);
                    thumb.setService(new ImageInformation(imageInfoURI));
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }

        if (getBuildMode().equals(BuildMode.IIIF)) {
            List<String> logoUrl = getLogoUrl();
            if(logoUrl.isEmpty()) {
                Optional<String> url = BeanUtils.getImageDeliveryBean().getFooter().getWatermarkUrl(Optional.empty(), Optional.ofNullable(ele), Optional.empty());
                url.ifPresent(l -> logoUrl.add(l));
            }
            for (String url : logoUrl) {
                ImageContent logo;
                try {
                    logo = new ImageContent(new URI(url));
                    manifest.addLogo(logo);
                } catch (URISyntaxException e) {
                    logger.error("Error adding manifest logo from " + url, e);
                }
            }
            
            for(String license : DataManager.getInstance().getConfiguration().getIIIFLicenses()) {
                try {
                    URI uri = new URI(license);
                    manifest.addLicense(uri);
                } catch(URISyntaxException e) {
                    logger.error("Configured license '" + license + "' is not a URI");
                }
            }

            String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
            if (StringUtils.isNotBlank(navDateField) && StringUtils.isNotBlank(ele.getMetadataValue(navDateField))) {
                try {
                    String eleValue = ele.getMetadataValue(navDateField);
                    LocalDate date = LocalDate.parse(eleValue);
                    manifest.setNavDate(Date.from(Instant.from(date.atStartOfDay(ZoneId.of("Z")))));
                } catch (NullPointerException | DateTimeParseException e) {
                    logger.warn("Unable to parse {} as Date", ele.getMetadataValue(navDateField));
                }
            }

            if (ele.isLidoRecord()) {
                /*LIDO*/
                try {
                    LinkingContent resolver = new LinkingContent(new URI(getLidoResolverUrl(ele)));
                    resolver.setFormat(Format.TEXT_XML);
                    resolver.setLabel(new SimpleMetadataValue("LIDO"));
                    manifest.addSeeAlso(resolver);
                } catch (URISyntaxException e) {
                    logger.error("Unable to retrieve lido resolver url for {}", ele);
                }
            } else {
                /*METS/MODS*/
                try {
                    LinkingContent metsResolver = new LinkingContent(new URI(getMetsResolverUrl(ele)));
                    metsResolver.setFormat(Format.TEXT_XML);
                    metsResolver.setLabel(new SimpleMetadataValue("METS/MODS"));
                    manifest.addSeeAlso(metsResolver);
                } catch (URISyntaxException e) {
                    logger.error("Unable to retrieve mets resolver url for {}", ele);
                }
            }

            /*VIEWER*/
            try {
                String applicationUrl = this.urls.getApplicationUrl();
                String pageUrl = ele.getUrl();
                LinkingContent viewerPage = new LinkingContent(new URI(applicationUrl + pageUrl));
                viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
                manifest.addRendering(viewerPage);
            } catch (URISyntaxException e) {
                logger.error("Unable to retrieve viewer url for {}", ele);
            }

            /*CMS pages*/
            try {
                DataManager.getInstance()
                        .getDao()
                        .getCMSPagesForRecord(ele.getPi(), null)
                        .stream()
                        .filter(page -> page.isPublished())
                        .forEach(page -> {
                            try {
                                LinkingContent cmsPage = new LinkingContent(new URI(this.urls.getApplicationUrl() + "/" + page.getUrl()));
                                //                    cmsPage.setLabel(new MultiLanguageMetadataValue(page.getLanguageVersions().stream()
                                //                            .filter(lang -> StringUtils.isNotBlank(lang.getTitle()))
                                //                            .collect(Collectors.toMap(lang -> lang.getLanguage(), lang -> lang.getTitle()))));
                                cmsPage.setLabel(new SimpleMetadataValue(page.getTitle()));
                                cmsPage.setFormat(Format.TEXT_HTML);
                                manifest.addRelated(cmsPage);
                            } catch (URISyntaxException e) {
                                logger.error("Unable to retrieve viewer url for {}", ele);
                            }
                        });
            } catch (Throwable e) {
                logger.warn(e.toString());
            }

            if (manifest instanceof Manifest) {
                /*PDF*/
                try {
                    String pdfDownloadUrl = BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(ele, manifest.getLabel().getValue().orElse(null));
                    LinkingContent pdfDownload = new LinkingContent(new URI(pdfDownloadUrl));
                    pdfDownload.setFormat(Format.APPLICATION_PDF);
                    pdfDownload.setLabel(new SimpleMetadataValue("PDF"));
                    manifest.addRendering(pdfDownload);
                } catch (URISyntaxException e) {
                    logger.error("Unable to retrieve pdf download url for {}", ele);
                }

            }
        }
    }

    /**
     * <p>
     * addVolumes.
     * </p>
     *
     * @param anchor a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @param volumes a {@link java.util.List} object.
     */
    public void addVolumes(Collection anchor, List<StructElement> volumes) {
        for (StructElement volume : volumes) {
            try {
                IPresentationModelElement child = generateManifest(volume);
                if (child instanceof Manifest) {
                    //                    addBaseSequence((Manifest)child, volume, child.getId().toString());
                    anchor.addManifest((Manifest) child);
                }
            } catch (ViewerConfigurationException | URISyntaxException | PresentationException | IndexUnreachableException | DAOException e) {
                logger.error("Error creating child manigest for " + volume);
            }

        }
    }

    /**
     * <p>
     * addAnchor.
     * </p>
     *
     * @param manifest a {@link de.intranda.api.iiif.presentation.Manifest} object.
     * @param anchorPI a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addAnchor(Manifest manifest, String anchorPI)
            throws PresentationException, IndexUnreachableException, URISyntaxException, DAOException {

        /*ANCHOR*/
        if (StringUtils.isNotBlank(anchorPI)) {
            manifest.addWithin(new Collection(getManifestURI(anchorPI), anchorPI));
        }

    }

    /**
     * @param v1
     * @return
     */
    private static Integer getSortingNumber(StructElement volume) {
        String numSort = volume.getVolumeNoSort();
        if (StringUtils.isNotBlank(numSort)) {
            try {
                return Integer.parseInt(numSort);
            } catch (NumberFormatException e) {
                logger.error("Cannot read integer value from " + numSort);
            }
        }
        return -1;
    }

    /**
     * Retrieves the logo url configured in webapi.iiif.logo. If the configured value is an absulute http(s) url, this url will be returned. If it is
     * any other absolute url a contentserver link to that url will be returned. If it is a non-absolute url, it will be considered a filepath within
     * the static images folder of the viewer theme and the appropriate url will be returned
     * 
     * @return An optional containing the configured logo url, or an empty optional if no logo was configured
     * @throws ViewerConfigurationException
     */
    private List<String> getLogoUrl() throws ViewerConfigurationException {
        List<String> urlStrings = DataManager.getInstance().getConfiguration().getIIIFLogo();
        List<String> logos = new ArrayList<>();
        for (String urlString : urlStrings) {
            try {
                URI url = new URI(urlString);
                if (url.isAbsolute() && url.getScheme().toLowerCase().startsWith("http")) {
                    logos.add(urlString);
                } else if (url.isAbsolute()) {
                    try {
                        String logo = imageDelivery.getIiif()
                                .getIIIFImageUrl(urlString, "-", RegionRequest.FULL.toString(), Scale.MAX.toString(), Rotation.NONE.toString(),
                                        Colortype.DEFAULT.toString(),
                                        ImageFileFormat.getMatchingTargetFormat(ImageFileFormat.getImageFileFormatFromFileExtension(url.getPath()))
                                                .toString());
                        logos.add(logo);
                    } catch (NullPointerException e) {
                        logger.error("Value '{}' configured in webapi.iiif.logo is not a valid uri", urlString);
                    }
                } else if (!StringUtils.isBlank(urlString)) {
                    logos.add(imageDelivery.getThumbs().getThumbnailPath(urlString).toString());
                }
            } catch (URISyntaxException e) {
                logger.error("Value '{}' configured in webapi.iiif.logo is not a valid uri", urlString);
                urlString = null;
            }
            
        }
        return logos;
    }

    /**
     * <p>
     * Getter for the field <code>buildMode</code>.
     * </p>
     *
     * @return the buildMode
     */
    public BuildMode getBuildMode() {
        return buildMode;
    }

    /**
     * <p>
     * Setter for the field <code>buildMode</code>.
     * </p>
     *
     * @param buildMode the buildMode to set
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder} object.
     */
    public ManifestBuilder setBuildMode(BuildMode buildMode) {
        this.buildMode = buildMode;
        return this;
    }

}