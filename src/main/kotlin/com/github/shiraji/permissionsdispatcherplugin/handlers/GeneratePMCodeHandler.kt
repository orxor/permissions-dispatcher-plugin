package com.github.shiraji.permissionsdispatcherplugin.handlers

import com.github.shiraji.permissionsdispatcherplugin.models.GeneratePMCodeModel
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

class GeneratePMCodeHandler(val model: GeneratePMCodeModel) : CodeInsightActionHandler {
    override fun startInWriteAction() = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        if (file !is PsiJavaFile) return
        addRuntimePermissionAnnotation(file)
        addNeedsPermissionMethod(file, project)
        if (model.isSpecialPermissions()) {
            addOnActivityResult(file, project)
        } else {
            addOnRequestPermissionsResult(file, project)
        }
        addOnShowRationale(file, project)
        addOnPermissionDenied(file, project)
        addOnNeverAskAgain(file, project)
    }

    private fun addOnNeverAskAgain(file: PsiJavaFile, project: Project) {
        if (model.onNeverAskAgainMethodName.length <= 0) return
        val methodTemplate = """void ${model.onNeverAskAgainMethodName}() {
        }""".trimMargin()

        val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(methodTemplate, file.classes[0])
        method.modifierList.addAnnotation("OnNeverAskAgain(${model.toPermissionParameter()})")
        file.importClass(model.createPsiClass("permissions.dispatcher.OnNeverAskAgain"))
        file.classes[0].add(method)
    }

    private fun addOnPermissionDenied(file: PsiJavaFile, project: Project) {
        if (model.onPermissionDeniedMethodName.length <= 0) return
        val methodTemplate = """void ${model.onPermissionDeniedMethodName}() {
        }""".trimMargin()

        val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(methodTemplate, file.classes[0])
        method.modifierList.addAnnotation("OnPermissionDenied(${model.toPermissionParameter()})")
        file.importClass(model.createPsiClass("permissions.dispatcher.OnPermissionDenied"))
        file.classes[0].add(method)
    }

    private fun addOnShowRationale(file: PsiJavaFile, project: Project) {
        if (model.onShowRationaleMethodName.length <= 0) return
        val methodTemplate = """void ${model.onShowRationaleMethodName}(PermissionRequest request) {
        }""".trimMargin()

        val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(methodTemplate, file.classes[0])
        method.modifierList.addAnnotation("OnShowRationale(${model.toPermissionParameter()})")
        file.importClass(model.createPsiClass("permissions.dispatcher.PermissionRequest"))
        file.importClass(model.createPsiClass("permissions.dispatcher.OnShowRationale"))
        file.classes[0].add(method)
    }

    private fun addOnRequestPermissionsResult(file: PsiJavaFile, project: Project) {
        val methods = file.classes[0].findMethodsByName("onRequestPermissionsResult", false)

        if (methods.size == 0) {
            val methodTemplate = """public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                ${file.classes[0].name}PermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
            }""".trimMargin()

            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(methodTemplate, file.classes[0])
            method.modifierList.addAnnotation("Override")
            file.classes[0].add(method)
        } else {
            val statement = "${file.classes[0].name}PermissionsDispatcher.onRequestPermissionsResult(this, ${methods[0].parameterList.parameters[0].name}, ${methods[0].parameterList.parameters[2].name});"
            val hasDelegation = methods[0].body?.text?.contains(statement) ?: false
            if (!hasDelegation) {
                val expression = JavaPsiFacade.getElementFactory(project).createStatementFromText(statement, file.classes[0])
                methods[0].body?.add(expression)
            }
        }
    }

    private fun addOnActivityResult(file: PsiJavaFile, project: Project) {
        val methods = file.classes[0].findMethodsByName("onActivityResult", false)
        if (methods.size == 0) {
            val methodTemplate = """public void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);
                ${file.classes[0].name}PermissionsDispatcher.onActivityResult(this, requestCode);
            }""".trimMargin()

            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(methodTemplate, file.classes[0])
            method.modifierList.addAnnotation("Override")
            file.importClass(model.createPsiClass("android.content.Intent"))
            file.classes[0].add(method)
        } else {
            val statement = "${file.classes[0].name}PermissionsDispatcher.onActivityResult(this, ${methods[0].parameterList.parameters[0].name});"
            val hasDelegation = methods[0].body?.text?.contains(statement) ?: false
            if (!hasDelegation) {
                val expression = JavaPsiFacade.getElementFactory(project).createStatementFromText(statement, file.classes[0])
                methods[0].body?.add(expression)
            }
        }
    }

    private fun addNeedsPermissionMethod(file: PsiJavaFile, project: Project) {
        if (model.needsPermissionMethodName.length <= 0) return
        val methodTemplate = """void ${model.needsPermissionMethodName}() {
        }""".trimMargin()

        val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(methodTemplate, file.classes[0])
        method.modifierList.addAnnotation("NeedsPermission(${model.toPermissionParameter()})")
        file.importClass(model.createPsiClass("permissions.dispatcher.NeedsPermission"))
        file.importClass(model.createPsiClass("android.Manifest"))
        file.classes[0].add(method)
    }

    private fun addRuntimePermissionAnnotation(file: PsiJavaFile) {
        if (file.classes[0].modifierList?.findAnnotation("permissions.dispatcher.RuntimePermissions") != null) return
        file.classes[0].modifierList?.addAnnotation("RuntimePermissions")
        file.importClass(model.createPsiClass("permissions.dispatcher.RuntimePermissions"))
    }

}