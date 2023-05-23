/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.completion;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;

public final class CompletionProposalTools {

	private CompletionProposalTools() {
		// to avoid instances, requested by sonar
	}

	/**
	 * The portion of the document leading up to the cursor that is being used as a
	 * filter for requesting completion assist
	 *
	 * @param document
	 * @param cursorOffset
	 * @param completionItemFilter
	 * @param completionInsertionOffset
	 * @return The longest prefix to the current cursor position that is found
	 *         within the completion's filter regardless of character spacing
	 */
	public static String getFilterFromDocument(Document document, int cursorOffset, String completionItemFilter,
											   int completionInsertionOffset) {
		if (completionInsertionOffset >= cursorOffset) {
			return ""; //$NON-NLS-1$
		}
		int prefixToCursorLength = cursorOffset - completionInsertionOffset;
		String prefixToCursor = document.getText(new TextRange(completionInsertionOffset, completionInsertionOffset + prefixToCursorLength));
		int i;
		for (i = 0; i < prefixToCursor.length(); i++) {
			if (!isSubstringFoundOrderedInString(
					prefixToCursor.substring(prefixToCursorLength - i - 1, prefixToCursorLength),
					completionItemFilter)) {
				break;
			}
		}
		return prefixToCursor.substring(prefixToCursor.length() - i);
	}

	/**
	 * If each of the character in the subString are within the given string in
	 * order
	 *
	 * @param subString
	 * @param string
	 */
	public static boolean isSubstringFoundOrderedInString(String subString, String string) {
		int lastIndex = 0;
		subString = subString.toLowerCase();
		string = string.toLowerCase();
		for (Character c : subString.toCharArray()) {
			int index = string.indexOf(c, lastIndex);
			if (index < 0) {
				return false;
			} else {
				lastIndex = index + 1;
			}
		}
		return true;
	}

	/**
	 * Uses the document's filter and the completion's filter to decided which
	 * category the match is.<br>
	 * Category 1:<br>
	 * The full completion filter is found within the document filter without a word
	 * characters as it's prefix or suffix<br>
	 * Category 2:<br>
	 * The full completion filter is found within the document filter without a word
	 * characters as it's prefix<br>
	 * Category 3:<br>
	 * The full completion filter is found within the document filter<br>
	 * Category 4:<br>
	 * {@link isSubstringFoundOrderedInString}(documentFilter, completionFilter) ==
	 * true<br>
	 * Category 5:<br>
	 * Catch all case, usually when all the document's filter's characters are not
	 * found within the completion filter
	 *
	 * @param documentFilter
	 * @param completionFilter
	 * @return the category integer
	 */
	public static int getCategoryOfFilterMatch(String documentFilter, String completionFilter) {
		if (documentFilter.isEmpty()) {
			return 5;
		}
		documentFilter = documentFilter.toLowerCase();
		completionFilter = completionFilter.toLowerCase();
		int subIndex = completionFilter.indexOf(documentFilter);
		int topCategory = 5;
		if (subIndex == -1) {
			return isSubstringFoundOrderedInString(documentFilter, completionFilter) ? 4 : 5;
		}
		while (subIndex != -1) {
			if (subIndex > 0 && Character.isLetterOrDigit(completionFilter.charAt(subIndex - 1))) {
				topCategory = Math.min(topCategory, 3);
			} else if (subIndex + documentFilter.length() < completionFilter.length() - 1
					&& Character.isLetterOrDigit(completionFilter.charAt(subIndex + documentFilter.length() + 1))) {
				topCategory = Math.min(topCategory, 2);
			} else {
				topCategory = 1;
			}
			if (topCategory == 1) {
				break;
			}
			subIndex = completionFilter.indexOf(documentFilter, subIndex + 1);
		}
		return topCategory;
	}

	/**
	 * Uses the document's filter and the completion's filter to decided how
	 * successful the match is and gives it a score.<br>
	 * The score is decided by the number of character that prefix each of the
	 * document's filter's characters locations in the competion's filter excluding
	 * document filter characters that follow other document filter characters.<br>
	 * <br>
	 * ex.<br>
	 * documentFilter: abc<br>
	 * completionFilter: xaxxbc<br>
	 * result: 5<br>
	 * logic:<br>
	 * There is 1 character before the 'a' and there is 4 charachters before the
	 * 'b', because the 'c' is directly after the 'b', it's prefix is ignored,<br>
	 * 1+4=5
	 *
	 * @param documentFilter
	 * @param completionFilter
	 * @return score of the match where the lower the number, the better the score
	 *         and -1 mean there was no match
	 */
	public static int getScoreOfFilterMatch(String documentFilter, String completionFilter) {
		documentFilter = documentFilter.toLowerCase();
		completionFilter = completionFilter.toLowerCase();
		return getScoreOfFilterMatchHelper(0, documentFilter, completionFilter);
	}

	private static int getScoreOfFilterMatchHelper(int prefixLength, String documentFilter, String completionFilter) {
		if (documentFilter == null || documentFilter.isEmpty()) {
			return 0;
		}
		char searchChar = documentFilter.charAt(0);
		int i = completionFilter.indexOf(searchChar);
		if (i == -1) {
			return -1;
		}
		if (documentFilter.length() == 1) {
			return i + prefixLength;
		}

		int matchLength = lengthOfPrefixMatch(documentFilter, completionFilter.substring(i));
		if (matchLength == documentFilter.length()) {
			return i + prefixLength;
		}
		int bestScore = i + getScoreOfFilterMatchHelper(prefixLength + i + matchLength,
				documentFilter.substring(matchLength),
				completionFilter.substring(i + matchLength));

		i = completionFilter.indexOf(searchChar, i + 1);
		while (i != -1) {
			matchLength = lengthOfPrefixMatch(documentFilter, completionFilter.substring(i));
			if (matchLength == documentFilter.length()) {
				return i + prefixLength;
			}
			int score = i + getScoreOfFilterMatchHelper(prefixLength + i + matchLength,
					documentFilter.substring(matchLength),
					completionFilter.substring(i + matchLength));
			if (score == i - 1) {
				break;
			}
			bestScore = Math.min(bestScore, score);
			i = completionFilter.indexOf(searchChar, i + 1);
		}
		return prefixLength + bestScore;
	}

	private static int lengthOfPrefixMatch(String first, String second) {
		int i;
		for (i = 0; i < Math.min(first.length(), second.length()); i++) {
			if (first.charAt(i) != second.charAt(i))
				break;
		}
		return i;
	}
}
