J2Free is a Java web framework that strives to remove the situps from using Java for web development. It began in 2007 and has continued on and off ever since.  Some of the projects that have contributed significantly to the codebase are FantasyCongress, Kingmaker, and PoliQuiz, all products of Publi.us, LLC, as well as JamLegend.com from FooBrew, Inc.

Most of the old code is undocumented and un-unit-tested, but the newer files tend to be better. Some features may be broken, though not deprecated, and some have become obsolete as other projects have provided better solutions. However, there a few that greatly eased the development and scaling of JamLegend.com.

Some of the most useful features are...

Fragment Cache (org.j2free.cache)
- Fully thread-safe in-memory HTML fragment caching with timeouts, cleaning, etc.  There has always been the intention of making a version that stored fragments in memcached / redis (probably redis, there's even a branch!), but we haven't gotten around to it.

QueuedHttpCallService
- Need to make external http calls?  Facebook / Twitter / Mixpanel API calls?  This service makes it easy to call external APIs asynchronously and is fully thread-safe.

EmailService
- This is a service for sending e-mail asynchronously that includes a dummy mode (for local dev), global headers, HTML and TXT templating, message prioritization, and customizable policies for send error handling. If it were updated to use ant StringUtils for substitution, it would be even more awesome.

Invoker
- We hate XML.  We hate editing the deployment descriptor whenever we want to add / remove / change a servlet / filter mapping.  So, we built an annotation-based servlet / filter mapping system with delegation handled by the InvokerFilter.  Some of this now exists in servlet spec 3 or Spring, but it was quite a relief in the fall of 2007.  Obviously, it's been upgraded significantly since then, and includes options for mapping servlets based on regex patterns, specifying exclusion patterns for filters, max servlet instance reuses, and more.

Configuration
- The ConfigurationListener makes it easy to configure the app at runtime, handling setting of config properties as application scope attributes, configuring a "RunMode", and enabling / configuring the FragmentCache, global executor service, email service, http service, and memcached connections (via spymemcached).

HoptoadNotifier
- Want to use hoptoad for organizing errors, even though they're designed for ruby?  It does a decent enough job.  This is built on top of the http service.

JSP tags and EL taglibs
- Various tools for various jobs.

Utils && Concurrent Utils
- Sequencers, data structures, HTML filtering, a pause-able executor, and some other random things.
