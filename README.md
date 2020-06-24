# DRES
The Distributed Retrieval Evaluation Server builds uppon the work of https://github.com/klschoef/vbsserver/ to provide the means to evaluate interactive retrieval approaches in various settings, both on-site and distributed.


## Submission
In order to submit a result to be evaluated, the submission endpoint is accessed via HTTP(S) in one of the following ways:
- http(s)://{server}/submit?*item*={item} where {item} is the identifier for the retrieved media item
- http(s)://{server}/submit?*item*={item}?*shot*={shot} where {shot} is the identifier for a pre-defined temporal segment within the {item}
- http(s)://{server}/submit?*item*={item}?*frame*={frame} where {frame} is the frame number within the {item}, in case it is a video
- http(s)://{server}/submit?*item*={item}?*timecode*={timecode} where {timecode} is a temporal position within the {item} in the form HH:MM:SS:FF. In case just a plain number is passed, the behavior is equivalent to passing the same value as {frame}
In case no session cookie is passed as part of the request, an additional *session* parameter can be passed to transmit the session token.
