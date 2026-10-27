# Tally ships with minification disabled. These rules are kept for completeness
# in case you enable R8 for a release build later.

# Keep the notification listener and widget receiver (referenced from the manifest).
-keep class com.tally.counter.service.TallyListenerService { <init>(); }
-keep class com.tally.counter.widget.TallyWidgetReceiver { <init>(); }
-keep class com.tally.counter.widget.ClearAllAction { <init>(); }
