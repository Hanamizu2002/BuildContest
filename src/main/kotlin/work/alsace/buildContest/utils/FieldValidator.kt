package work.alsace.buildContest.utils

/**
 * FieldValidator 对象用于验证输入字段是否符合指定的正则表达式规则。
 */
object FieldValidator {

    /**
     * 验证输入字段是否符合指定的正则表达式。
     *
     * @param fieldName 字段名称，用于错误提示。
     * @param value 要验证的字段值。
     * @param regex 用于验证的正则表达式。
     * @return 验证结果，符合返回 true，否则返回 false。
     */
    fun validate(fieldName: String, value: String, regex: Regex): Boolean {
        return if (regex.matches(value)) {
            true
        } else {
            println("Invalid $fieldName format: $value does not match the required pattern.")
            false
        }
    }
}
