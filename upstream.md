# LSP4MP 0.5.0 -> 0.6.0

| Commit                                       | Title                                                                       |
|----------------------------------------------|-----------------------------------------------------------------------------|
| ~~1c5b7f8a27663a46414f827adf956bddf7b7aca2~~ | Changelog for 0.5.0                                                         |
| ~~05e33ee4030cdc4447531e59e51cb80107a2f79a~~ | Upversion to 0.6.0-SNAPSHOT                                                 |
| ~~e3e36c469e09be43454cb9796198461390bf070c~~ | Move VS Code workspace configuration into correct folder                    |
| ~~944f5483af90625593f4ba5f2b7e76b78d668427~~ | Update JDT-LS to 1.14.0 Release.                                            |
| ~~1dd10583a7da5d845c569119e8d3c0376c86a40a~~ | Use computeModelAsyncCompose from quarkus-ls                                |
| ~~13c5c0e863214509bb68b7096e0b866329f0123~~  | Display property expression evaluation as inlay hint                        |
| 492d78d57eec119b79100a197652d7265b0410ff     | Use SmallRye Expression library to resolve property expressions             |
| ~~4020308f44e217d9707d82e5417097791fe926a5~~ | fix: JDT.LS optional in test bundle                                         |
| ~~64ed772c58a6061de9102dffa54dc12e752ffd3b~~ | Update Target Platform to 1.16.0-SNAPSHOT version of JDT-LS target.         |
| ~~b8c6d0651ae7c59f137a59a240fde3f40874a2ca~~ | Use eclipse.jdt.ls 1.16.0-SNAPSHOT in TP configurator                       |
| 2e2626c856af1fbb7e4811185fd03162efdc3d90     | Add diagnostics for mp-reactive-messaging @Incoming/Outgoing annotation     |
| ~~8a90f38e744558c2fcccbf26a4cc1150e25b5c01~~ | Prevent ClassCastException for invalid @ConfigProperty default value        |
| ~~5cf8de4874bd0ec9904c411c871f4813b024ac96~~ | Show value when hovering unrecognized property                              |
| f5571c37f71799acd5c0a0a14b43b735bee83313     | Support CodeAction resolve for Java code actions                            |
| 8ccfef8087a3279bcb15cf92f9d093e861dd4d19     | Provide support for config_ordinal property                                 |
| ~~69b0a631cbf13d33fb311154ecb4ac0e47da4e16~~ | Display diagnostic/inlay hint in Java file after loading of MP Java project |
| 1d61bd6b254bce1be2fd9c6b79805034890b8f82     | Manage static properties using a staticProvider extension point             |
| ~~db43b5a09b18b7ed2ca170c52f0ecb793ee25a2f~~ | Use FQN when checking annotation type in `AnnotationUtils`                  |
| ~~2d7d129f545151f123ff935893a0853959164d34~~ | Fix inlay hint + definition when project returns empty properties.          |
| 167d09e9979acd2418b86be60380dfeb2a55c589     | Add REST client CodeLens for additional annotations                         |
| bbc010aff9121abb23679d818fc5e7749688c1be     | Improve JAXRS code lens range to method name                                |
| bfcef6433402ae9e27adf48609d8b4c91e0ec685     | Check for `HealthCheck` instead of `Health` before validation               |
| 10d3e73cb686a6fd2e06b6c23279d004d9a1ae65     | Address AIOOBE from JAX-RS code lens                                        |
| ~~b7d2f9520a8b2eb545ceb9e3952d2823dc322fba~~ | Use JavadocContentAccess2                                                   |
| ~~dbe33f03c9e36a9c110c6ef6d07d717d7c156bc6~~ | Fix typo in "mpirc" snippet.                                                |
| ~~d8e6f87600531736216028acdd656fbf9b756781~~ | Revert "Use JavadocContentAccess2"                                          |
| ~~730d27b76513210de7ec0f31dcc1319182d63481~~ | Changelog for 0.6.0                                                         |
| ~~97d4e6c9372da7661d7a1e5a9f52580ce8887322~~ | Release 0.6.0                                                               |

# quarkus-ls 0.12.1 -> 0.13.0

| Commit                                       | Title                                                                                   |
|----------------------------------------------|-----------------------------------------------------------------------------------------|
| ~~d2023227d069fb286643143a807abb7217a14c83~~ | Upversion to 0.13.0                                                                     |
| 8a52980f2a999e80a0367f301f8962dfaf4f84b7     | CodeAction to generate missing property in Qute template                                |
| ~~69c93ad7f06007df63438c40757b86b64b29d4cb~~ | Fix cancel support with CompletableFuture compose                                       |
| 03eec61dd9a6e13d1c487ae041e5174d7b30aa9a     | Added similar text suggestion Code Action(s) for UndefinedObject and UndefinedNamespace |
| 779b31a89527ac7dc446c70b09d32ae91f2de726     | Don't show "add field/getter" CodeActions in Qute for binary types                      |
| 03b06df7be5f591dd597b18da9c7e7025927fa1a     | Create a code action class per error code.                                              |
| ~~41da2c76e4c037d213c8c204b7a6d3474a0df04b~~ | Do not give CodeActions for stale diagnostics                                           |
| ~~782939d67e82927845eeafa5feb780ea604faf61~~ | Update dependencies to latest versions.                                                 |
| 635bedf3bc738108f21c228af27dd6d8d22fd8b8     | Generate one CodeAction for each template extensions class                              |
| ~~11c5583d1356b7e8099a6d7dab5466b659daeb35~~ | Resolve rename requests to `null` in Java files for qute-ls                             |
| ~~8bd86c7d1bb1aef8948803ee24754e3eb1dd7006~~ | Added Qute CodeAction(s) for similar text suggestions for UnknownMethod                 |
| ~~52bd23a425d0b03e296c6c4136a5b12445ecb4d0~~ | Clickable InlayHint for Java type                                                       |
| ~~37bb5f95f533d72fb09c817a85ac634c3684adf5~~ | codeAction/resolve support for qute-ls                                                  |
| 918823e95346e2e1d08fac6da958bf660e98e162     | Provide validation for data model for #switch section                                   |
| ~~c76ddfb1219cef1cb31206c023babfa888e60c76~~ | ClassCastException with code action and method part                                     |
| ~~9b5fe029af4c6d2ec2b80240be6d91fbdd3043de~~ | Check with qualified name of Template fields in AbstractQuteTemplateLinkCollector       |
| 2f4fc508a3b51c6668f382f6dce73d8eb838d913     | Ignore synthetic method in Qute template                                                |
| ~~53220714c05a193c6d9c18eb9efd55c4027b8538~~ | Added Qute CodeAction(s) for similar text suggestions for `UnknownProperty`.            |
| c3d012c84991b2869b2ac2f14c90d2289ac9e6e1     | Provide completion for enum in #switch section                                          |
| ~~65c6a8803d0b51f39e81c13cafa0e6f3395cdc22~~ | Provide definition for Enum in #switch section                                          |
| ~~285c2240135ac9e57d3d57b66c431dfb60df1f9d~~ | Indent snippet new line if LSP client doesn't support InsertTextMode#AdjustIndentation  |
| ~~a1aafd7a65e2007dd45e6ec21a12b798cdbe2051~~ | Validate operator in #case and #is section                                              |
| 3a3ba32b9b7f001085986d5ad586fec914147ce1     | Prevent infinite recursion in validation, completion, code actions                      |
| 9aafe08c4f58d1a5bdcc2056c0d68b701d2202df     | Add check to prevent invalid inject items in completion                                 |
| ~~856eb28ac2b456ca0a50f3389670eeb50a6aa3d2~~ | Support completion for operator in #case #is sections                                   |
| ~~50f76d70e5c49ab96374b8838b58f17d2a5207ab~~ | Fix missing definition for #case section Enum with operator name                        |
| ~~bdafad0b1fdacbe4d450a0cb0b24462ee44a27b2~~ | Fix Qute parser to parse operator parameters with '=' correctly                         |
| ~~cace274a1405940689060c1a7cfd869d29ba9b1d~~ | Update Target Platform to 1.16.0-SNAPSHOT version of JDT-LS target.                     |
| ~~2fcfb402f3c377fd6a36cb591929001ae1ecd7d0~~ | Support hover for operators in #case section                                            |
| ~~9eac792eb2110792d1b2c7771218de990f530eff~~ | fix: Missing exports from quarkus test bundle                                           |
| 77fca54181a5af110d1a2d419eba39d37a45534c     | Generic support for Java data model                                                     |
| ~~2da7df28aa2f9e8a7adeb6865aeec3bea280181f~~ | Prevent duplicate completion items in Qute templates                                    |
| ed5e29ae65842e592d6ff8ab278c178b132ae08b     | Documentation when hovering object members in Qute                                      |
| ~~c73fdd070e8cca5fccec84af487214542743f94c~~ | Fix tests by taking into account new property from lsp4mp                               |
| ~~267113c9e4811fdecb3d910c158449ff222d493d~~ | Add new snippet `qitrc` to create an integration test class                             |
| fc960a4e849ef88bfebd1cb3e346b3c34b3d306c     | Fix JAX-RS code lens test failures                                                      |
| ~~b8c559c03d029973a389d033f11e0e7bb1c3edac~~ | Changelog for 0.13.0                                                                    |

# LSP4MP 0.4.0 -> 0.5.0

| Commit                                       | Title                                                                           |
|----------------------------------------------|---------------------------------------------------------------------------------|
| ~~9fa64f9c61ec678fc58702cbd3c87aa339814c5e~~ | Bump Changelog date for 0.4.0 Release.                                          |
| ~~d18b9bd6670fdcd5bf4785894de8f878163cc010~~ | Upversion to 0.5.0-SNAPSHOT                                                     |
| ~~b502df2af2896dec4555a89cb3a26cadad156d5c~~ | Validate default value expression                                               |
| ~~f54b7355be2797ec8843e7d106cb4af0296a2ba6~~ | fix: plugin.xml missing from test bundle JAR                                    |
| ~~04c8293af85acda7e4adacf90c469b356f50d7db~~ | Bump quarkus.platform.version to 2.9.0.Final in all-quarkus-extensions          |
| ~~d77a4176cfe4073763702f49b594f23467bcadca~~ | Improved MicroProfile property value expression diagnostic message              |
| a687e95b98ddd221b547d5ca3341e49bbb6d1303     | renamed project to ordinal                                                      |
| c35634c5fc6ab2664db3101051a900486bf30512     | Added JDTTypeUtils.isVoidReturnType for QuarkusConfigMappingProvider void check |
| ~~71aa7f31d393dfe369c5072e9748ed64e320caa5~~ | Ignored ENV reference in expression validation                                  |
| ~~2d7c54fb9c04d640bc778d51bac0300be970fb33~~ | Temporarily use JDT-LS 1.12.0 (instead of SNAPSHOT build)                       |
| fd561a8d05fcd26cc3f7c8ad8fc9a0d0d703d721     | Moved PropertyReplacerStrategy to right package                                 |
| ~~1f570471f1d880d88186a951883948e74c36f5f1~~ | Remove unnecessary 2019-06 release repository from target platform.             |
| ~~0cc3f88f655c8dd15686d7a5fa05c31da28b1b17~~ | corrected path to javaASTValidator in schema                                    |
| ~~1d0377596b08e453c609bf5d0a0451af553a1e32~~ | Delay revalidation and handle cancellation better                               |
| ~~c855f69611efb661972790581374e441c5d6ca0e~~ | Add check to ensure prefix is from ConfigProperties                             |
| 63da0d68e7c498ab126c04782faf9bf3a8b43f2d     | Update lsp4j 0.14.0                                                             |
| 7e37ad88a8b1c5c33f218d7c25f06b74f41dd213     | Add checks to prevent ClassCastException for incorrect retry value              |
| ~~357fd47e6867492f3811aa50908ae5bedbe471df~~ | Support completion before '='                                                   |
| ~~e0a4a3ce3f97ea98292f5cc614401b4f4c96e5be~~ | Check for cancellation requests in Java files, delay revalidation               |
| 1279cbc2b081f059485dd99a1f1b477d728d6ef3     | Add check to prevent failure on incorrect attribute values                      |
| cdc3219ca6c7d1d98c45cb8ee496f6229938f84b     | Update ASTValidator to use project classpath                                    |
| ~~73ccbb63252ccedfdd168cc467710a1afa8f2b16~~ | Update jdt-ls to 1.13.0, fix compile errors                                     |
| ~~9d5349fb50b72888b2b1815b374219feff586d68~~ | Update Gradle test project versions to 7.3                                      |
| ~~b11a8ea130f2d453dfe0947d96ceac97ee210080~~ | Remove the m2e lifecycle mapping plugin from MANIFEST.MF                        |
| ~~f8167fdfbc4a6cb5006bcc12a1aa096761e7e997~~ | Fix NPE when hovering in properties file                                        |
| ~~65cc5112fb531264d33a8f26b85e0b40875ab987~~ | Fix application properties completion with exisiting property value             |
| ~~90b74b169e9831cb158ef293cabf99f7baeca10d~~ | Do not give CodeActions for stale diagnostics                                   |
| ~~14ea29641888e338bdfa9c690b5635f936b5884c~~ | Remove unnecessary Gson dependency in pom file.                                 |
| ~~1c5b7f8a27663a46414f827adf956bddf7b7aca2~~ | Changelog for 0.5.0                                                             |
| ~~7f6bc76a58d0b3c371956f213c7890e13381fc79~~ | Release 0.5.0                                                                   |

# quarkus-ls 0.11.1 -> 0.12.1

| Commit                                       | Title                                                                                         |
|----------------------------------------------|-----------------------------------------------------------------------------------------------|
| ~~59a67746d537a64cd41bd5867e283d6d46764521~~ | Upversion to 0.12.0-SNAPSHOT                                                                  |
| ~~78783b9692c8c475ff6c05169d5f2678079da8d7~~ | build: Archive P2 update site on stable folder on download.jboss.org                          |
| ~~7a123e2be613a99e503ef052e54043334cedcf0e~~ | Added CodeAction to append ?? for UndefinedObject and ignored optional object from diagnostic |
| ~~3a9f9c2ac7c77ac80e83f5463225959926ed72e9~~ | Don't show undefined variable errors depending on context                                     |
| ~~cec54cfaa7dc1900ab9042e113e4616a63353808~~ | Support for `textDocument/InlayHint`                                                          |
| ~~709147c1224d8bcf4de0dd88991f5969012ef2ee~~ | Update Quarkus LS to use LSP4MP 0.5.0 Snapshots.                                              |
| ~~7c27abdc0afb7af89139f95b6f39c400c9f24da1~~ | build: update download.jboss.org IP to reflect infra changes                                  |
| ~~f7ef2e8df53ff5d88628284d7cdc43c6164e60d1~~ | Expression indexes are wrong                                                                  |
| 17d1792c31626e13c26aed117381cd680318544b     | Provide qute.native.enabled setting                                                           |
| 970e1172c48417c3335bc9040fccc40a8c4c0ea3     | Added static field and method support for @TemplateData                                       |
| 4f93bae1eb69342aa14e45d59e7a02d813ef423c     | Added support for Qute @TemplateEnum annotation                                               |
| ~~460534a06c70b4e908d2d1bba82ac16bbc2cb4ba~~ | Linked editing doesn't work if variable is used as a parameter into a section                 |
| ~~703b82a0e6a6ff13601e2d023d0eca7a93e9d0f1~~ | template validation complains about strings containing spaces                                 |
| ~~b1271618bc7b308fd88f3c60c6f0f1be3c2c1564~~ | Support for global variables.                                                                 |
| 9dd5a92aab304bf55d5808e4daf6ffefa1c836ea     | Added support for @TemplateGlobal annotation                                                  |
| 30915cdf34ebe6bf2e11ad5c2ea2ab4885b05875     | Fixed method void return check in QuarkusConfigPropertiesProvider                             |
| 95d808aa2104508ebd962f90c441088776b374d0     | Varargs are not properly supported in Qute template validation                                |
| 4f7a13c1e1af920e5b0a6c38c8e32b5ce5c497c5     | Support missing attributes for @TemplateData / @RegisterForReflection                         |
| b92fc6947c57dde80d92cf5b03c3444c414e5357     | Simplify resolve signature.                                                                   |
| ~~42d8954ba8c46b0e0ff1dadd8759591bc6a5a710~~ | Temporarily use JDT-LS 1.12.0 (instead of SNAPSHOT build)                                     |
| ~~2c2b342ef20b03b77a3693faea62c0e2168e9263~~ | Move to LSP4j 0.13.0                                                                          |
| ~~1f40476113ae9a996f5131d78fa250fece4d20a0~~ | Added rename support for Qute templates                                                       |
| ~~512deb06449a2ca6c1f7925ccce0d5b6da5ba59a~~ | Moved PropertyReplacerStrategy to correct package                                             |
| ~~f554944d00fb16371f4929e1e496560d40a6d8d2~~ | Remove unnecessary 2019-06 release repository from target platform.                           |
| ~~4fa1495d189fe2f4d33f8a7614acb999629b88a7~~ | Update lsp4j to 0.14.0                                                                        |
| ~~b5a04317e0363a33b08303d1635ef1a31eb54f30~~ | Delay revalidation after edit and improve cancel checking                                     |
| ~~daaa1d062b3aacac5326b7a8c4b69e41e6f847cd~~ | Improved completion when nested in Qute if section                                            |
| ~~de5cbe66f34c6a52ae60a1725dbaadc8d9464325~~ | Add DCO documentation                                                                         |
| ~~dbcaf2a3b457d5558725ec1128695d889d76dea0~~ | Fix NPE with data model template                                                              |
| ~~402a40c6ab56795c7781fe2100e490d3f59eb4db~~ | Improved completion when nested in Qute for, switch and when sections                         |
| ~~9690993c641b0bab007df6ce8bc2eec13a64384f~~ | Improve TemplateScanner performance                                                           |
| ~~0eb94978c4f804c2cc9de8a15fe482e5bfaf80dc~~ | Delay revalidation of Java files in qute-ls                                                   |
| ~~9cc52b0ecbdc7b17e689c87d7301161aebfa7960~~ | Added nested else completion support for when section                                         |
| ~~7797648ba41b0d92714e9e90ca7020dabdbf2d49~~ | Adapt to new version of m2e in JDT-LS                                                         |
| bb2d2c9c7bcac2124ffb82438652e6e3df458e32     | When Template is constructor-injected, @Location is ignored                                   |
| ~~fc083c8d7e458b7f3222d6f63c381662a9a41c77~~ | Update Jenkinsfile to use Java 17                                                             |
| ~~e12966c62b22210c3c1fbefc9924e8626c29c15f~~ | Remove unnecessary Gson dependency in pom files                                               |
| ~~f72e937fe0695f398f214f0faa415ccecac86c74~~ | Changelog for 0.12.0                                                                          |
| ~~1564c624e2fe4c40148fafa86b92b1224518126a~~ | Use JDK 11 for nexus deployment plugins.                                                      |

# LSP4MP 0.3.0 -> 0.4.0

| Commit                                       | Title                                                                                                               |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| c5916d852367d741447efc5da0e22675370dc856     | Validate defaultValue of @ConfigProperty                                                                            |
| 4bfbdd4ac51363dab45102bdd357205030e094f7     | Extension point for contributing configuration sources                                                              |
| 36a2dba1d1188fba39e9951e63f0f569e9bdc790     | Do not include fallback annotated method as its own fallback option                                                 |
| f9afc25b0ba3083251fd60cbca976bb3603d0750     | Remove `QuarkusConfigSourceProvider`                                                                                |
| ddbaf89c2f91c7d27597adf4122e40d3c0b69045     | Warn when a @ConfigProperty is declared but no value is assigned                                                    |
| 51d5e8715c27a05c439fd84fe10e9cf3fd7b8bef     | [MP Config 2.0] Completion for properties defined using @ConfigProperties                                           |
| 392f092dc70ec2687d0edaaa297c3c426e1c904b     | added diagnostic for fault-tolerance Asynchronous annotation                                                        |
| ~~9d00aaf30d3c466156f5e392d7e4e54313b455a5~~ | @ConfigProperty code action when property is not set                                                                |
| bf096364555a8380cd043e6096bbc1d9f2434e53     | added diagnostic for fault-tolerance Asynchronous annotation marked on type declaration                             |
| 9e086cd99fc29083898f6035b0dd7dcb9123e8bc     | added quarkus.http.root-path property to CodeLensURL                                                                |
| ~~ce6cc7704a2865f416d50760edf32cb6bfc6eb9a~~ | Run LSP4J callback methods in separate thread                                                                       |
| ~~36ad9e723405e4ca97da5fdca338ca4ad830fc7f~~ | Fix class cast exception                                                                                            |
| 8531fee6cd5bb2fb60429f98d5bb72a432103258     | Diagnostics for invalid annotation parameter values                                                                 |
| ~~fd508c6b2efc5c9b75e3355a6c88ef427dcff4b1~~ | Exclude unassigned with code action                                                                                 |
| ~~3569fb1bc3394aa50cb80457c0bbae6bf2bbf967~~ | SingleMemberAnnotation diagnostics not supported by annotationValidator NOT APPLICABLE                              |
| bcdd1578905946bb97a4c4997df0cf67191561d2     | added Bulkhead and Timeout non-negative diagnostic support                                                          |
| b4a99898a3d4258b7c53331f9c26ed56e43b8603     | added diagnostics support for Retry annotation member values                                                        |
| 4518fd728ec5747e22839f1f63aa82ee6d06187e     | added diagnostic for ConfigProperty name member as empty string                                                     |
| 9bbe254a7f13e384f2819fde37b54a88a64fbb19     | [MP Config 2.0] Support for config profiles                                                                         |
| 5780d0b38d441eb6531e61e7dfa4dde37b9ce68d     | fixed ChronoUnit static import case for Retry annotation unit                                                       |
| 585f2f9e5bbadbae1d91c8e752850349cd087b76     | Improve readability of MicroProfileMetricsDiagnosticsParticipant                                                    |
| 8a43ca5548af6a157f473f75b42c0fcdedd940f5     | Improve readability of MicroProfileMetricsDiagnosticsParticipant (2)                                                |
| 3d9d3832935f7fb22cd2190abf3f20987749402d     | removed Duration class from Retry annotation and supported date based ChronoUnit                                    |
| ~~5f98c0cf130edd06e6446cc6a45003dba72d5f94~~ | Support for default value inside properties expression                                                              |
| 9e82e2ffaf359a79906c3041c5982fbdb5efa812     | support ApplicationPath annotation value in CodeLens URL                                                            |
| ~~2a141a29a874c741616a956fc0eb3946a316b273~~ | 2a141a29a874c741616a956fc0eb3946a316b273                                                                            |
| ~~b2d94a4889f1967be5d9bad2449426354b38af2c~~ | b2d94a4889f1967be5d9bad2449426354b38af2c                                                                            |
| 4f573c31769bb90e2f766c7ba66581b7c0b7652d     | moved ApplicationPath search engine to JaxRsContext                                                                 |
| 63555f586f21e296684636c22a9817848cddb0fb     | support optional property reference hover for annotation members                                                    |
| ~~bd97344aa464fec51984b6472e766c12972ce084~~ | Do not rebuild list of configuration properties when MicroProfile config sources are updated in the build directory |
| ec5f1ab23b1d3f08f2bd6b60f37bea15a3069d6d     | added property replacer to annotation definition participant                                                        |
| ~~67aec322569d328c3bd1544f9402c387163956e5~~ | -> Update o.e.jdt.ls.tp dependency to 1.7.0 Release                                                                 |
| ~~629db87933daa85f72917af2c69d042c616ce5cc~~ | Bump junit from 4.12 to 4.13.1 in /microprofile.ls/org.eclipse.lsp4mp.ls                                            |
| ~~6fd300d6622a9ebd7a466308970b92b2b938d84a~~ | projectLabels command doesn't support jdt:// URIs                                                                   |
| ~~9ad1e396c8fcc3e128d93a67f9bd246821c2b8a6~~ | CHANGELOG for 0.4.0                                                                                                 |
| ~~9fa64f9c61ec678fc58702cbd3c87aa339814c5e~~ | Bump Changelog date for 0.4.0 Release.                                                                              |
| ~~433b426fcce8d6bf2e59680f29ca8a9f4b86beab~~ | Release 0.4.0                                                                                                       |

# quarkus-ls 0.10.1 -> 0.11.1

| Commit                                       | Title                                                                                           |
|----------------------------------------------|-------------------------------------------------------------------------------------------------|
| ~~c210ebfd551d84d74191b704b75d50be0fd449cd~~ | Add a step to the Jenkinsfile to checkout the repo                                              |
| ~~4b2cc115d9e4cd540acb725a81b61cdc27429f6a~~ | Upversion to 0.10.2                                                                             |
| 7d4b5da470d839eec52d9bcd7e47d13f8785a634     | Provide completion for cache name in configuration file                                         |
| 3dbed3e2c02cc4f02ed0f31178fd2ece13242d12     | Move application.properties and application.yaml support to quarkus-ls                          |
| d931b173582344dad9357a0ecd1180af0984b718     | Support `application-${profile}.properties`                                                     |
| 9688dc9994b43d1dac2086302221574d556d16d4     | added quarkus.http.root-path property retrieval to Quarkus JaxRs CodeLens                       |
| 209e7463ecbe7d8c58f81cfe20137019c075049b     | [MP Config 2.0] Support for config profiles                                                     |
| f8fdb05b4242e5f958b0ccbdec5bc148d1486376     | added diagnostics and member validation for quarkus Scheduled annotation                        |
| 49275a9256f6c6ccd175ba18b81cb76f633bcb5a     | added test for ApplicationPath annotation and quarkus.http.root-path in CodeLensURL             |
| 45a9ea5d77704fa2b6cb583bc1dbfc3107b8bb65     | Support for @ConfigMapping                                                                      |
| ~~f65af982bda329dd9b62bc0320fd99fae6abecea~~ | Updated jdt.ls target platform version to 1.5.0                                                 |
| e57ffbff57ac63a3a8ba99b4a987c7526968f1c9     | Validate that @ConfigMapping annotation can only be placed in interfaces                        |
| ~~79fe7b977bc4c72a38e9c65fc8b3cf2a7a4d3300~~ | [Qute] Create a Qute Language Server                                                            |
| ~~ee0422a26ba7004ad9cd102169eff419dd04ec98~~ | Fix exec flag on buildAll.sh                                                                    |
| 9ef3427f529e2bf68b4cebe651bde14def6ea48d     | added property replacement parameter for Scheduled cron attribute property expression           |
| ~~9ed49e190698ca717d3cb34736488092c918e484~~ | [qute] parser lenient about new parameters                                                      |
| ~~130fe9d853d7fc6c995e55b5d58c6a3202ab88f0~~ | [qute] Codelenses disappear once @CheckedTemplate is followed by parenthesis                    |
| ~~8d0959154ec35fc6e6bf0240118bfb53cc5fdc96~~ | Non tag reported as error                                                                       |
| ~~8645a4361425e1783b3cb5b3503f0afa1da3f879~~ | Add Qute artifacts deployment                                                                   |
| ~~ed2b933955762a26390e931f87f50e8557580605~~ | Fix buildAll.sh, again                                                                          |
| ~~83b34871ea205bb1efa63354161b2a16e94dbd2d~~ | Don't show data model completion inside tag section                                             |
| ~~10d204feb305a656ab4fa7104357631e139d30cf~~ | Duplicate items when completing subclass of an object                                           |
| ~~afbcdfea4d24ac69b958613e755f34966d831e9e~~ | Parameter declaration support improvement                                                       |
| ~~61928619b8cfed0b8bfbda0aee8d188d71cd313d~~ | Template sees parameters leaked from other templates                                            |
| ~~bdda69484fecd8826e376be0e7a9fd924e5f032f~~ | Records are not supported in qute template                                                      |
| 196ae430cd4665f223d579d83e3f71d3f77c4a64     | added definition support for Scheduled annotation                                               |
| ~~3c020ae947cf0d29277830c23d906ce9a38e567d~~ | Support {/} end tag section correctly                                                           |
| ~~3337d58103dcad2ed42ec34c18f7ce76f2e463fb~~ | Update o.e.jdt.ls.tp dependency to 1.7.0-SNAPSHOT                                               |
| ~~278e2ed393d85aff8e2a7419a6714b5844ad5d9f~~ | Make clickable CodeLens data model                                                              |
| ~~8c06be8766da9c247da5a31536902410e40e5401~~ | Filter methods completion with Qute rules                                                       |
| ~~e1fab1a0faba37e1b27b9242b24d93af6b4c52ab~~ | Qute validation easily goes out-of-sync when link-editing                                       |
| ~~2a69247ef100ca0c0c6bc1c7cc5343d51051ea4f~~ | Iteration metadata support not properly implemented                                             |
| ~~dfebc6efa9cbf25b00245aa082e83c1cfaf83cfd~~ | Fix completion with @TemplateExtension                                                          |
| ~~edaf4cc119554358d4d2629c799c2d6d7ad58b84~~ | Fixed NPE on Qute LS shutdown on Eclipse IDE                                                    |
| ~~709cb6ea909ed6bea6430c52e7ffa3aee7d98cc7~~ | Loop parameter leak into for-else block                                                         |
| ~~136a9a2aaf08708aeb6902eeb1607665b4db393c~~ | Changed Qute embedded Java completion to use simple types                                       |
| ~~954bc9e9e1a13d38b0ca4df7beed62550b5ee8c5~~ | Support for orEmpty                                                                             |
| ~~885db0e9f647b2007784605c5c1083308d28cbc2~~ | Support for method parameters                                                                   |
| ~~9a2f1d0d5a6662beca4fd387eb66b55c9d1184e8~~ | Validate method parameters for template extension                                               |
| ~~dbf5f0244359f7535a4cebdc9fd5f5d817c142af~~ | Validate overloaded methods                                                                     |
| ~~92208fb5b74de76b6f82d20fea2396f0a7b07161~~ | Validation fail when iterating over integers                                                    |
| ~~d1477abad4b6c7fd91c2262dae0cc492fcecf065~~ | Support for #if expression                                                                      |
| ~~2bf93d42598894fa8b01b99d67ffdab395d5e1d0~~ | NPE in linked editing when deleting variable                                                    |
| ~~aff6277327d33e5152c9afb8fad30bba7fa4b9a4~~ | StringIndexOutOfBoundsException with {@}                                                        |
| ~~8db2e198dec00e6aa63477f9c58339dfdc3af574~~ | Literal support as object                                                                       |
| ~~43b999f8dbbfdd5ba3fb74e13d32ee5aa5366a18~~ | When used with @Location, templates validation fail                                             |
| ~~b47de335fa535f584b1f865c9a420b637f9c004b~~ | Added or and ifTruthy Qute resolver support                                                     |
| ~~e5505ee9d943f224842493ee100bc2013c1bd470~~ | Added support for Qute template Java simple type hover                                          |
| 1121a2d8abc455aa4d6555dacb0a8b3023c9ee27     | Use SafeConstructor for Yaml parser instantation.                                               |
| ~~da79fdb2413dd5e73764d426f682201dd9982156~~ | Update o.e.jdt.ls.tp dependency to 1.7.0 Release                                                |
| ~~49379fbf91c4e5e0bc9c87a3d5ed4243f6211e8c~~ | Add proper array support in Qute templates                                                      |
| ~~81844da254766de127e0e4b99ab2ccc6b4a16fe4~~ | CodeAction to disable validation                                                                |
| ~~216728ab05d05a9dc3a938841320b63b73f29e2c~~ | Remove quarkus.tools suffix for Qute settings.                                                  |
| ~~c173f99f60053fe3ae2b04fbcfd7425edd5c47ce~~ | Added support for Qute raw and safe character escape value resolvers                            |
| ~~76ab820fd35fa30a9996d460a8f15dd4392b1bc3~~ | Settings from workspace folder                                                                  |
| ~~e9f298bb08c317c6f49eb53763808c411861ed10~~ | Generate .html Qute templates by default                                                        |
| ~~1cb5381a79b23fac2e87540da46544908feeb96a~~ | Support exclusion file for Qute for validation                                                  |
| ~~f0fd4596bf669dee0c1697c4e610bb28fbe126fd~~ | Use Qute 2.7.0                                                                                  |
| ~~0623415d7e0bfc3a165095e627263383aebe5303~~ | Support AST for parameter declaration not closed                                                |
| ~~0a2b75e0a89efbf5f4ac69bf770d00f9fd8563c5~~ | Completion for available section after { and {#                                                 |
| ~~b48d50dd7d003f7795d97ce5a243b030fcf38c27~~ | NPE on textDocument/definition on unbalanced section                                            |
| ~~87103ede0c97b37564d1a80e27f0fd32fb8b1b66~~ | Support for user tag                                                                            |
| ~~234a64cf62dbe0d69b450dbb6a6df7ace6f92c8b~~ | Ignore undefined variable diagnostic for user tag                                               |
| ~~38fadc0596f125c436d903bd84f2b5ab7dd5715b~~ | Support parameter expression in user tag                                                        |
| ~~cfe356affb321a8f9e9d8e24b7ff67948f834652~~ | Support for untitled uri.                                                                       |
| ~~70e6df17a307de94122781e6b7b08045e145123e~~ | User tag codelens support                                                                       |
| ~~c757aafddf091454b2bb70c5e237a9f32eddedb1~~ | Code action for undefined section tag                                                           |
| ~~7760f65adb969f217257a516c67914a5f9896f25~~ | Special keys support for user tag                                                               |
| ~~3ff73946e0220c8b9eb0b858c7b11c3913e1bce9~~ | build: fix cleanup of qute.jdt update site                                                      |
| ~~bb24e783fbc13504e5af7ed4932deff0d320b2ef~~ | fix(test): export tests and refactor QuteProjectTest utility                                    |
| ~~3e82e069f06c7dc40bad67a7f518c2ccdb6f954d~~ | fix(test): skip Java 17 related tests                                                           |
| ~~9c48330144a6eef07205ab4366f868a5bd753498~~ | fix: prevent NPE in Qute LS if LSP client does not implement getUserTags                        |
| ~~44672fce66c046c39b8e76dc2c06f26b55680112~~ | Support for Injecting Beans Directly In Templates                                               |
| ~~b247849e376493117f3ab704d9f3060003ce86f0~~ | Need a NodeVisitor class to traverse Qute AST Nodes                                             |
| ~~116027020fde710ff7e63936e0be1b127ed24bd4~~ | Support parameter value as String                                                               |
| ~~1cd78213ce8fc4dfe6d780bb6cce62de8573593b~~ | Support for @TemplateExtension matchName                                                        |
| ~~d9b8f830448bc2fccaee4c68939909c1885e4946~~ | Replaced UndefinedVariable by adding validation severity setting for Qute UndefinedObject error |
| ~~2de60965c7edb9d1f89c2718bdf8815cffe226b4~~ | Support Java expression for it user tag                                                         |
| ~~04f7c7bf922749ab90e3a5be5c5d9391d4e5ac47~~ | Added CodeAction to set UndefinedObject severity to ignore                                      |
| ~~8ffbc2f0810b20fa6604776c1419f0f7a475c54d~~ | Qute-LS crashes if settings.json is configured improperly                                       |
| ~~b16a72ca1a445e2e5256ce21289aa970e5cfacc8~~ | Rename code action to "Ignore `UndefinedObject` problem."                                       |
| ~~574f58fcf37d020f4e493e523e8e0303c0e73fc8~~ | StringIndexOutOfBoundsException is thrown when computing getter methods                         |
| ~~9634c7594137fe12f6b827c548269ee11da2e8d3~~ | Added UndefinedNamespace severity setting and CodeAction                                        |
| ~~44759252eb62c75e1bd5be560c01c8746736aa9c~~ | Catch error when collect data model is process for a given provider.                            |
| ~~d6a7219414d7fbc9f0619372d837acd66a723cd3~~ | Wrong method parameter resolution (ignores subtypes)                                            |
| ~~e1197c43f1b27ab184c6ce130ee16383145f4062~~ | Support enable/disable CodeLens setting for Qute templates                                      |
| ~~31817e379390b170ed0a6facff8a3996196cd560~~ | Fix bug with super method.                                                                      |
| ~~966c1865ed187f81dea057ba3b5ef6e8510fc94b~~ | Support for Infix notation                                                                      |
| ~~b3d103104e07809dd866b9dde463cab925023cc8~~ | Changelog for 0.11.0                                                                            |
| ~~a10416dc86d9acf0028257dafbb25f1296c34c9a~~ | Bump nexus-staging-maven-plugin from 1.6.8 to 1.6.12                                            |

# LSP4MP 0.2.1~~ 0.3.0

|Commit|Title|
|------|-----|
|6429bf901311c23f95b410d797e8acedceae3357|completion for `fallbackMethod` (Fault Tolerance)|

# LSP4MP 0.2.0~~ 0.2.1

RAS (P2 release only)

# LSP4MP 0.1.0~~ 0.2.0

|Commit|Title|
|------|-----|
|b88710cc54170844717f655b9bff8bb4c4649a8d|add JULPropertiesProvider and JBossLogManagerPropertyProvider|
|d13833cb6b95fb3955425a7b08b989089024c269|implement fileInfo|
|0922a1067da18b47d4848d03fcccfaf055838413|profiles values for config name hover|
|8b501176faab0c3af457075fbc86ea061c23a223|fix on property name scanning|
|b7f56b6445632eb0ee9b6bd9fa439dd0397d50cd|navigation from Java @ConfigProperty to application.properties (new Java defininition participant)|
|6f2d700a88a3262e39cc2ba04beedb429e162246|refactor MavenProjectName and GradleProjectName|
|acaebd9069ad5160d8ea7f0b7ec0388b3559daa6|tests refactoring|
|5b6d0e38fafde56f35bb4f3fbb6774b06224dbcb|PropertiesHoverParticipant refactoring|
|bc926f75df2ca103d78c67b997c87adb7ab480b1|fix in PropertiesHoverParticipant|
|b116fb50a53c47d7a06519d1b8eea15176f06d1c|fix in MicroProfileReactiveMessagingProvider|
