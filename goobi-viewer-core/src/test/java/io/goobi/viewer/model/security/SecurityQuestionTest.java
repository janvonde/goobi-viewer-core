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
package io.goobi.viewer.model.security;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class SecurityQuestionTest {

    /**
     * @see SecurityQuestion#isAnswerCorrect(String)
     * @verifies return true on correct answer
     */
    @Test
    public void isAnswerCorrect_shouldReturnTrueOnCorrectAnswer() throws Exception {
        SecurityQuestion q = new SecurityQuestion("foo", Collections.singleton("answer"));
        Assert.assertTrue(q.isAnswerCorrect("answer"));
    }

    /**
     * @see SecurityQuestion#isAnswerCorrect(String)
     * @verifies return true on correct answer and ignore case
     */
    @Test
    public void isAnswerCorrect_shouldReturnTrueOnCorrectAnswerAndIgnoreCase() throws Exception {
        SecurityQuestion q = new SecurityQuestion("foo", Collections.singleton("answer"));
        Assert.assertTrue(q.isAnswerCorrect("ANSWER"));
    }

    /**
     * @see SecurityQuestion#isAnswerCorrect(String)
     * @verifies return false on incorrect answer
     */
    @Test
    public void isAnswerCorrect_shouldReturnFalseOnIncorrectAnswer() throws Exception {
        SecurityQuestion q = new SecurityQuestion("foo", Collections.singleton("answer"));
        Assert.assertFalse(q.isAnswerCorrect("wronganswer"));
    }

    /**
     * @see SecurityQuestion#isAnswerCorrect(String)
     * @verifies return false empty answer
     */
    @Test
    public void isAnswerCorrect_shouldReturnFalseEmptyAnswer() throws Exception {
        SecurityQuestion q = new SecurityQuestion("foo", Collections.singleton("answer"));
        Assert.assertFalse(q.isAnswerCorrect(null));
        Assert.assertFalse(q.isAnswerCorrect(""));
        Assert.assertFalse(q.isAnswerCorrect("   "));
    }

    /**
     * @see SecurityQuestion#isAnswerCorrect(String)
     * @verifies mark question as answered
     */
    @Test
    public void isAnswerCorrect_shouldMarkQuestionAsAnswered() throws Exception {
        SecurityQuestion q = new SecurityQuestion("foo", Collections.singleton("answer"));
        Assert.assertFalse(q.isAnswered());
        q.isAnswerCorrect("wrong");
        Assert.assertTrue(q.isAnswered());
    }
}
