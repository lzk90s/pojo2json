package ink.organics.pojo2json;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiUtil;
import ink.organics.pojo2json.fake.JsonFakeValuesService;

import java.util.*;
import java.util.stream.Collectors;

public class POJO2JsonCommentAction extends POJO2JsonAction {

    @Override
    protected Object getFakeValue(JsonFakeValuesService jsonFakeValuesService) {
        return jsonFakeValuesService.def();
    }

    @Override
    protected Object typeResolve(PsiType type, PsiDocComment comment, int level) {

        level = ++level;

        if (type instanceof PsiPrimitiveType) {
            //primitive Type
            return buildFullComment(type, comment);
        } else if (type instanceof PsiArrayType) {
            //array type
            List<Object> list = new ArrayList<>();
            PsiType deepType = type.getDeepComponentType();
            list.add(typeResolve(deepType, comment, level));
            return list;
        } else {
            //reference Type
            Map<String, Object> map = new LinkedHashMap<>();
            PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(type);

            if (psiClass == null) {
                return map;
            }

            if (psiClass.isEnum()) {
                return buildFullComment(type, comment);
            } else {

                List<String> fieldTypeNames = new ArrayList<>();

                PsiType[] types = type.getSuperTypes();

                fieldTypeNames.add(type.getPresentableText());
                fieldTypeNames.addAll(Arrays.stream(types).map(PsiType::getPresentableText).collect(Collectors.toList()));

                if (fieldTypeNames.stream().anyMatch(s -> s.startsWith("Collection") || s.startsWith("Iterable"))) {
                    List<Object> list = new ArrayList<>();
                    PsiType deepType = PsiUtil.extractIterableTypeParameter(type, false);
                    list.add(typeResolve(deepType, comment, level));
                    return list;
                } else {
                    // Object
                    List<String> retain = new ArrayList<>(fieldTypeNames);
                    retain.retainAll(normalTypes.keySet());
                    if (!retain.isEmpty()) {
                        return buildFullComment(type, comment);
                    }

                    if (level > 500) {
                        throw new KnownException("This class reference level exceeds maximum limit or has nested references!");
                    }

                    for (PsiField field : psiClass.getAllFields()) {
                        map.put(fieldResolve(field), typeResolve(field.getType(), field.getDocComment(), level));
                    }
                    return map;
                }
            }
        }
    }

    private String buildFullComment(PsiType type, PsiDocComment comment) {
        return unifyType(type.getCanonicalText()) + " //" + getComment(comment);
    }

    private String getComment(PsiDocComment comment) {
        return Optional.ofNullable(comment).map(PsiElement::getText)
                .map(s -> s.replaceAll("\\*", "").replaceAll("/", "").strip())
                .orElse("");
    }

    private String unifyType(String className) {
        String[] cPaths = className.replace("[]", "").split("\\.");
        String rawType = cPaths[cPaths.length - 1];
        if ("byte".equalsIgnoreCase(rawType)) {
            return "Byte";
        } else if ("short".equalsIgnoreCase(rawType)) {
            return "Short";
        } else if ("int".equalsIgnoreCase(rawType)
                || "Integer".equalsIgnoreCase(rawType)
                || "BigInteger".equalsIgnoreCase(rawType)) {
            return "Integer";
        } else if ("long".equalsIgnoreCase(rawType)) {
            return "Long";
        } else if ("float".equalsIgnoreCase(rawType)) {
            return "Float";
        } else if ("double".equalsIgnoreCase(rawType)
                || "BigDecimal".equalsIgnoreCase(rawType)) {
            return "Double";
        } else if ("boolean".equalsIgnoreCase(rawType)) {
            return "Boolean";
        } else if ("char".equalsIgnoreCase(rawType)
                || "Character".equalsIgnoreCase(rawType)) {
            return "Character";
        } else if ("String".equalsIgnoreCase(rawType)) {
            return "String";
        } else if ("date".equalsIgnoreCase(rawType)
                || "LocalDate".equalsIgnoreCase(rawType)
                || "ZonedDateTime".equalsIgnoreCase(rawType) || "LocalDateTime".equals(rawType)) {
            return "Date";
        } else if ("file".equalsIgnoreCase(rawType) || "MultipartFile".equalsIgnoreCase(rawType)) {
            return "File";
        } else if ("Object".equalsIgnoreCase(rawType)) {
            return "Object";
        } else if ("enum".equalsIgnoreCase(rawType)) {
            return "Enum";
        } else {
            return rawType;
        }
    }
}
