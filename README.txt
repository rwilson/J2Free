J2Free is the mildly unorganized, mostly undocumented, highly un-unit-tested framework upon which JamLegend is built.  Annoyed with constantly copying code from old projects when moving on to new ones, we consolidated some of the more used items in this library.  Often, this was because we wanted to perform some X asynchronously, and writing a whole asynchronous routine each time is highly redundant. Several portions of the library rely heavily on apache-commons code.

Some features we don't use anymore; some may even be broken.  However, there are a few we actively use and could not do without.

The following are the features we actively use on JamLegend:

Fragment Cache (org.j2free.cache)
- A fully thread-safe in-memory fragment caching with timeouts, cleaning, etc.  There has always been the intention of making a version that stored fragments in memcached / redis (probably redis), but we haven't gotten around to it.

QueuedHttpCallService
- Need to make external http calls?  Facebook / Twitter / Mixpanel API calls?  This service makes it easy to call external APIs asynchronously and is fully thread-safe.

EmailService
- This is a service for sending e-mail asynchronously that includes a dummy mode (for local dev), global headers, HTML and TXT templating, message prioritization, and customizable policies for send error handling.

Invoker
- We hate XML.  We hate editing the deployment descriptor whenever we want to add / remove / change a servlet / filter mapping.  So, we built an annotation-based servlet / filter mapping system with delegation handled by the InvokerFilter.  Some of this now exists in servlet spec 3, and in Spring, but in the fall of 2007 when we first got this going, it was quite a relief.  Obviously, it's been upgraded significantly since then, and includes options for mapping servlets based on regex patterns, specifying exclusion patterns for filters, max servlet instance reuses, and more.

Config
- The ConfigurationListener makes it easy to configure the app at runtime, handling setting of config properties as application scope attributes, configuring a "RunMode", and enabling / configuring the FragmentCache, global executor service, email service, http service, and memcached connections (via spymemcached).

HoptoadNotifier
- We at JamLegend use hoptoad for organizing our errors, even though they're designed for ruby.  It does a decent enough job.  This is built on top of the http service.

JSP tags and EL taglibs
- Various tools for various jobs.

Utils && Concurrent Utils
- Sequencers, data structures, HTML filtering, a pause-able executor, and some other random things.