# Spring Mvc Logger

## 采用Spring AOP 实现 Controller 自动打印日志

**日志格式**

> invoke service=`Controller类名`.`方法名`, params=[`参数1json`|`参数2json`|...], result=[`返回结果json`], use time=`耗时ms`

参数部分支持两种内部提示:
 - `<Ignored>`:  使用```@LoggerIgnore```注解的参数，或参数为内置不支持的参数类型，`HttpServletRequest`，`HttpServletResponse`
 - `<Error>`: 序列化为json时报错
 
例如：
```
invoke service=com.github.logger.LoggerController.test1, params=[<Ignored>|"world"|], result=["test1"], use time=1ms
```

**@LoggerIgnore使用**

该注解用于禁用logger

用于Controller方法上，该方法将不输出日志

```
    @LoggerIgnore
    @DeleteMapping("/test")
    public String test4() {
        return "test4";
    }
```

用于Controller方法的参数上，该参数将不输出到日志，使用`<Ignored>`占位

```
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String test1(@LoggerIgnore @RequestParam(required = false) String x,
        @RequestParam(required = false) String y) {
        return "test1";
    }
```