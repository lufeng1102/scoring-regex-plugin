# nutch [scoring-regex-plugin]


nutch scoring plugin to generate high score for specific url

1. add following settings to conf/nutch-site.xml
```
<property>
	<name>scoring.regex.file</name>
	<value>scoring-regex.txt</value>
	<description>regex file for scoring</description>
</property>
```

2. add followng setting into build deploy and clean property in src/plugin/build.xml of nutch source

```
<target name="deploy">
	...
	<ant dir="scoring-regex" target="deploy"/>
	....
</target>

<target name="clean">
	...
	<ant dir="scoring-regex" target="clean"/>
	...
</target>
```

3. add this plugin into plugin.includes property on conf/nutch-site.xml
```
<property>
	<name>plugin.includes</name>
	<value>..|scoring-regex|...</value>
</property>
```

4. config the setting file on conf/scoring-regex.txt and format like this
 
```
# url regex
http://www.dianping.com/shop/[0-9]+ 5
```
here use space as delimiter to split the url regex and increase rate for these urls.
note that order matters, and only first matching rule applies

5. run the testing
 
 ```
$ bin/nutch plugin scoring-regex org.apache.nutch.scoring.regex.RegexAnalysisScoringFilter
```