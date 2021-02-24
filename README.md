## Start als docker deamon im Hintergrund

```
docker run --restart unless-stopped -d -p 8080:8080 -v /var/cache/ebooks/:/var/books/preview/ -v /home/thomas/Downloads/eBooks/:/opt/bookserver/ebooks/ book-download-server:0.2
```
