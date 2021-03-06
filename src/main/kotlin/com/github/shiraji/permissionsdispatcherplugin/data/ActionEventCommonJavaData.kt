package com.github.shiraji.permissionsdispatcherplugin.data

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile

data class ActionEventCommonJavaData(val file: PsiJavaFile, val editor: Editor, val element: PsiElement, val clazz: PsiClass)