#import "members.typ": members

#let template(body) = [
  #let title = "
   COMP3911 Secure Computing
   Coursework 2
   "
  
  #set document(title: title)

  #align(center)[
    #[
      #set text(16pt, weight: "bold")
      #title
    ]
    #members()
  ]

  #set heading(numbering: "1.1.1")

  #pagebreak()

  #set page(numbering: "1", number-align: center + bottom)
  #counter(page).update(1)
  #body
]