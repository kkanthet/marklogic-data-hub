xquery version "1.0-ml";

module namespace err = "http://marklogic.com/data-hub/err";

import module namespace functx = "http://www.functx.com"
  at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

declare option xdmp:mapping "false";

declare %private function err:to-camel($str)
{
  let $toks := fn:tokenize($str, "-")
  return
    fn:string-join((
      $toks[1],
      fn:subsequence($toks, 2) ! functx:capitalize-first(.)
    ), "")
};

declare function err:error-to-json($error as element(error:error))
{
  let $o := json:object()
  let $_ :=
    for $e in $error/*[fn:not(self::error:stack or self::error:data)]
    return
      map:put($o, err:to-camel(fn:local-name($e)), fn:data($e))
  let $_ :=
    let $data := json:array()
    let $_ :=
      for $datum in $error/error:data/error:datum
      return
        json:array-push($data, fn:string($datum))
    return
      map:put($o, "data", $data)
  let $_ :=
    let $stack := json:array()
    let $_ :=
      for $frame in $error/error:stack/error:frame
      let $f := json:object()
      let $_ :=
        for $e in $frame/*[fn:not(self::error:variables)]
        return
          map:put($f, err:to-camel(fn:local-name($e)), fn:data($e))
      let $_ :=
        let $variables := json:object()
        let $_ :=
          for $variable in $frame/error:variables/error:variable
          return
            map:put($variables, $variable/error:name/fn:string(), $variable/error:value/fn:data(.))
        return
          map:put($f, "variables", $variables)
      return
        json:array-push($stack, $f)
    return
      map:put($o, "stacks", $stack)
  return
    $o
};
