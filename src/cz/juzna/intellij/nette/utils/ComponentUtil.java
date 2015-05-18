package cz.juzna.intellij.nette.utils;


import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class ComponentUtil {

	private static PhpType container = new PhpType().add("Nette\\ComponentModel\\Container");
	public static String factoryMethodPrefix = "createComponent";

	public static boolean isContainer(PhpClass csl) {
		return container.isConvertibleFrom(csl.getType(), PhpIndex.getInstance(csl.getProject()));
	}

	@NotNull
	public static Method[] getFactoryMethods(PsiElement el) {
		return getFactoryMethods(el, false);
	}

	@NotNull
	public static Method[] getFactoryMethods(PsiElement el, boolean onlyWithName) {
		PhpType type = null;
		String componentName = null;

		if (el instanceof ArrayAccessExpression) {
			ArrayIndex index = ((ArrayAccessExpression) el).getIndex();
			if (index == null || !(el.getFirstChild() instanceof PhpTypedElement)) {
				return new Method[0];
			}
			componentName = ElementValueResolver.resolve(index.getValue());
			type = ((PhpTypedElement) el.getFirstChild()).getType();

		} else if (el instanceof MethodReference) {
			MethodReference methodRef = (MethodReference) el;
			if (methodRef.getClassReference() == null
					|| methodRef.getName() == null
					|| !methodRef.getName().equals("getComponent")
					|| methodRef.getParameters().length != 1) {
				return new Method[0];
			}
			componentName = ElementValueResolver.resolve(methodRef.getParameters()[0]);
			type = methodRef.getClassReference().getType();
		}
		if (type == null || type.isEmpty() || type.toString().trim().equals("") || (componentName == null && onlyWithName)) {
			return new Method[0];
		}

		Collection<Method> methods = new ArrayList<Method>();
		for (PhpClass currentClass : PhpIndexUtil.getClasses(type, PhpIndex.getInstance(el.getProject()))) {
			methods.addAll(getFactoryMethods(currentClass, onlyWithName ? componentName : null));
		}
		Method[] result = new Method[methods.size()];

		return methods.toArray(result);
	}

	public static Collection<Method> getFactoryMethods(@NotNull PhpClass cls, String componentName) {
		Collection<Method> methods = new ArrayList<Method>();
		if (!isContainer(cls)) {
			return methods;
		}
		if (componentName != null) {
			String method = factoryMethodPrefix + StringUtil.upperFirst(componentName);
			Method m = cls.findMethodByName(method);
			if (m != null) {
				methods.add(m);
			}
		} else {
			for (Method method : cls.getMethods()) {
				if (method.getName().startsWith(factoryMethodPrefix) && method.getName().length() > factoryMethodPrefix.length()) {
					methods.add(method);
				}
			}
		}
		return methods;
	}


	@Nullable
	public static String methodToComponentName(String methodName) {
		if (!methodName.startsWith(factoryMethodPrefix) || methodName.length() <= factoryMethodPrefix.length()) {
			return null;
		}
		return StringUtil.lowerFirst(methodName.substring(factoryMethodPrefix.length()));
	}

}
