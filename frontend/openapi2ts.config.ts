/**
 * @umijs/openapi 配置文件
 *
 * 作用：根据后端 SpringDoc OpenAPI 接口文档，自动生成前端 TS 请求代码与类型声明。
 * 使用：npm run openapi2ts
 *
 * 说明：
 * - 生成的请求函数和类型会输出到 ./src/api/aicomposer/ 目录，不会覆盖现有手写的 src/api/userController.ts。
 * - 后端接口文档地址：http://localhost:8567/api/v3/api-docs（需先启动后端服务）。
 */
export default {
    // 后端 OpenAPI 文档地址（SpringDoc 默认路径 /v3/api-docs）
    schemaPath: 'http://localhost:8567/api/v3/api-docs',
    // 生成目录（工具会在该目录下再建一层 projectName 子目录）
    serversPath: './src/api',
    // 子目录名，生成的代码输出到 ./src/api/aicomposer/
    projectName: 'aicomposer',
    // 请求方法导入语句，使用项目自己的 axios 实例（src/request.ts）
    requestLibPath: "import request from '@/request'",
    // 类型命名空间（与 src/types/api.d.ts 一致，可合并扩展）
    namespace: 'API',
    // 小驼峰命名文件和请求函数
    isCamelCase: true,
    // 用 type 而不是 interface 声明类型
    declareType: 'type',
    // 字符串字面量枚举
    enumStyle: 'string-literal',
}
