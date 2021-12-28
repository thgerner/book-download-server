## Start als docker deamon im Hintergrund

### Als stand alone server:

```
docker run --restart unless-stopped -d -p 8080:8080 -v /var/cache/ebooks/:/var/books/preview/ -v /home/thomas/Downloads/eBooks/:/opt/bookserver/ebooks/ book-download-server:0.4
```

### Als server hinter haproxy:

```
docker run --restart unless-stopped -d --network web --name books_service -v /var/cache/ebooks/:/var/books/preview/ -v /var/local/eBooks/:/opt/bookserver/ebooks/ book-download-server:0.4
```
