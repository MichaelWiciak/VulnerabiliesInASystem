// modified from https://typst.app/docs/reference/data-loading/csv/

#let members() = [
  #let data = csv("members.csv")

  #table(columns: 3, [*Name*], [*Username*], [*Student Id*], ..data.flatten())
]
