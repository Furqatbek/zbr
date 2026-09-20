# Reply to Restos — round ten

**Draft for review.** Short. Nothing in it needs a decision.

---

We had your test gap too, and found it because you wrote yours down.

## The same hole, one layer over

You pinned `ACCEPTED` and left `PREPARING` and `READY` resting on a mapping
nobody had asserted reached us. Ours counted calls: both reporting tests checked
that six or three went out and passed `anyString()` for the status itself — so
the mapping from our enum to your vocabulary was unpinned for **seven of the
nine** states we send. Only `DELIVERED` and `CANCELLED` were nailed by name, and
incidentally, in tests about other things.

Now captured and asserted by name, in order.

**Checked rather than assumed**, since a test that claims to pin something is
worth less than one that has been seen to fail: lowercasing the status the
service sends breaks five of these tests. Before the change it broke none of
them.

Your description is what made it findable — *correct in the code and unexamined
in the tests* is a thing you can go and look for, where "we have good coverage"
is not.

## The sentence worth keeping

> *do not tell a kitchen what it just did* is a true sentence about one path and
> a false rule about the system.

That is the better statement of what we got wrong, and it generalises past this
bug. The fix was not a cleverer list of redundant states; it was answering a
different question — who caused this — which has one answer and cannot drift.
We have put that reasoning next to the code rather than in a commit message,
for the same reason you did.

## Your correction, accepted

Our Stage 1 is not an advantage we earned so much as a different shape of debt:
you keep outbound credentials in configuration and therefore never had the
plaintext column we closed. Noted, and the design constraint you drew from it is
the right one — when it moves into a row it moves in encrypted, authenticated,
key outside the database.

**Your rotation being a deployment is worse than ours being an admin call**, and
worth saying plainly because it is easy to file as equivalent. A thing that
requires a deploy is a thing nobody does on the day they should, which is the
day it matters. Ours is reissue-and-re-enter and will not survive ten partners;
yours will struggle at two.

## Crossed in the post

Your owed-ticket question — *what value do you record against your copy?* — is
answered in our round nine, which we sent before yours arrived. Short version:
the goods at the price you published, no delivery fee, no service fee of ours,
no tip. Your definition, and now a column rather than a promise.

## On writing these

Agreed, and we would not have predicted it either. Four rounds ago this was
about field names. The last three findings were each one side reading the
other's account of its own system and noticing something the owner could not
see — which is an argument for the descriptions being written by the people who
built the thing, and read by the people who did not.
