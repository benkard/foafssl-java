This directory contains the N3 rules to prove the contents of the paper.

1. Proof of the traditional CA method
-------------------------------------

to test this run


$ cwm  proof1.n3 ont.n3 http://www.w3.org/2000/10/swap/test/owl/owl-rules.n3 --think --solve=goal2.n3
     @prefix : <proof1.n3#> .
    :client     = <ldap:subjectDN> .

This just returns the identity between the handle the client has, and the global handle that is available
in the CA signed certificate. Essentially it shows that the server knows "you are <ldap:subjectDN> .
(todo: make that a better ldap name)

To look at how it comes to this conclusion see:(it is very long)

$ cwm  proof1.n3 ont.n3 http://www.w3.org/2000/10/swap/test/owl/owl-rules.n3 --think --solve=goal2.n3 --why > out.n3

For more information about what the reasoner ends up knowning about client see

$ cwm  proof1.n3 ont.n3 http://www.w3.org/2000/10/swap/test/owl/owl-rules.n3 --think --solve=goal.n3 

