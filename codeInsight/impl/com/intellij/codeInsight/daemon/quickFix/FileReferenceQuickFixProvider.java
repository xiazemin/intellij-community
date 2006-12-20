package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction;
import com.intellij.codeInsight.daemon.impl.quickfix.RenameFileFix;
import com.intellij.ide.highlighter.UnknownFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.IncorrectOperationException;
import com.intellij.codeInspection.LocalQuickFix;

import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

/**
 * @author Maxim.Mossienko
 */
public class FileReferenceQuickFixProvider {
  private FileReferenceQuickFixProvider() {}

  public static List<? extends LocalQuickFix> registerQuickFix(final HighlightInfo info, final FileReference reference) {
    final FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();
    int index = reference.getIndex();

    if (index < 0) return Collections.emptyList();
    final String newFileName = reference.getCanonicalText();

    // check if we could create file
    if (newFileName.length() == 0 ||
        newFileName.indexOf('\\') != -1 ||
        newFileName.indexOf('*') != -1 ||
        newFileName.indexOf('?') != -1 ||
        SystemInfo.isWindows && newFileName.indexOf(':') != -1) {
      return Collections.emptyList();
    }

    final PsiFileSystemItem context;
    if(index > 0) {
      context = fileReferenceSet.getReference(index - 1).resolve();
    } else { // index == 0
      final Collection<PsiFileSystemItem> defaultContexts = fileReferenceSet.getDefaultContexts(reference.getElement());
      if (defaultContexts.isEmpty()) {
        return Collections.emptyList();
      }
      context = defaultContexts.iterator().next();
    }
    if (context == null) return Collections.emptyList();

    final PsiDirectory directory = reference.getPsiDirectory(context);

    if (directory == null) return Collections.emptyList();

    boolean differentCase = false;

    if (fileReferenceSet.isCaseSensitive()) {
      boolean original = fileReferenceSet.isCaseSensitive();
      try {
        fileReferenceSet.setCaseSensitive(false);
        final PsiElement psiElement = reference.resolve();

        if (psiElement instanceof PsiNamedElement) {
          final String existingElementName = ((PsiNamedElement)psiElement).getName();

          differentCase = true;
          final RenameFileReferenceIntentionAction renameRefAction = new RenameFileReferenceIntentionAction(existingElementName, reference);
          QuickFixAction.registerQuickFixAction(info, renameRefAction);

          final RenameFileFix renameFileFix = new RenameFileFix(newFileName);
          QuickFixAction.registerQuickFixAction(info, renameFileFix);
          return Arrays.asList(renameRefAction, renameFileFix);
        }
      } finally {
        fileReferenceSet.setCaseSensitive(original);
      }
    }

    if (differentCase && SystemInfo.isWindows) return Collections.emptyList();

    final boolean isdirectory;

    if (!reference.isLast()) {
      // directory
      try {
        directory.checkCreateSubdirectory(newFileName);
      } catch(IncorrectOperationException ex) {
        return Collections.emptyList();
      }
      isdirectory = true;
    } else {
      FileType ft = FileTypeManager.getInstance().getFileTypeByFileName(newFileName);
      if (ft instanceof UnknownFileType) return Collections.emptyList();

      try {
        directory.checkCreateFile(newFileName);
      } catch(IncorrectOperationException ex) {
        return Collections.emptyList();
      }

      isdirectory = false;
    }

    final CreateFileIntentionAction action = new CreateFileIntentionAction(isdirectory, newFileName, directory);
    QuickFixAction.registerQuickFixAction(info, action);
    return Arrays.asList(action);
  }

}
