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
package io.goobi.viewer.model.crowdsourcing.questions;

/**
 * The TargetSelector determines which part of a source (usually a iiif manifest or other collection of iiif canvases) is the target for an annotation
 * generated by a {@link io.goobi.viewer.model.crowdsourcing.questions.Question}
 *
 * @author florian
 */
public enum TargetSelector {
    /**
     * Target is the entire source, usually a iiif manifest
     */
    WHOLE_SOURCE,
    /**
     * Target is the canvas of the current page
     */
    WHOLE_PAGE,
    /**
     * Target is a rectangular area within the current canvas, described by a FragmentSelector
     */
    RECTANGLE;

}